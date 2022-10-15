package fr.redboxing.athena;

import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingOptions;
import br.com.azalim.mcserverping.MCPingResponse;
import fr.redboxing.athena.command.CommandManager;
import fr.redboxing.athena.database.DatabaseManager;
import fr.redboxing.athena.database.entities.Server;
import fr.redboxing.athena.manager.ServerManager;
import fr.redboxing.athena.utils.ThreadFactoryHelper;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

public class AthenaBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(AthenaBot.class);
    private static final String MC_COLOR_REGEX = "§[0-9a-fk-or]";

    @Getter
    private static AthenaBot instance;

    @Getter
    private JDA jda;
    private final Queue<String> scanningQueue = new ConcurrentLinkedQueue<>();

    @Getter
    private final CommandManager commandManager;
    private Process p;
    private final List<Thread> scanningThreads = new ArrayList<>();
    private final HashMap<String, HashMap<String, Long>> cooldowns = new HashMap<>();
    private boolean refreshing = false;
    @Getter
    private final ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        instance = new AthenaBot();
    }

    public AthenaBot() {
        this.scheduler = new ScheduledThreadPoolExecutor(12, new ThreadFactoryHelper());

        this.commandManager = new CommandManager();
        this.commandManager.loadCommands(this);

        DatabaseManager.getSessionFactory();

        JDABuilder builder = JDABuilder.createDefault(BotConfig.get("BOT_TOKEN"));
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        builder.setBulkDeleteSplittingEnabled(true);
        builder.setCompression(Compression.NONE);
        builder.setActivity(Activity.playing(ServerManager.getTotalServerCounts() + " server scanned !"));
        builder.addEventListeners(new EventListener(this));

        this.jda = builder.build();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public boolean start(String portRange) {
        if(this.p != null) return false;
        Thread scan = new Thread(() -> this.scan(portRange));
        scanningThreads.add(scan);
        scan.start();

        for(int i = 0; i < 12; i++) {
            Thread t = new Thread(() -> {
                while(true) {
                    if(this.scanningQueue.isEmpty()) continue;
                    String address = scanningQueue.poll();
                    if(address == null) continue;

                    this.refresh(address);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            scanningThreads.add(t);
            t.start();
        }

        return true;
    }

    public void stop() {
        if(this.p == null) return;
        this.p.destroy();
        this.p = null;

        for(Thread t : this.scanningThreads) {
            t.interrupt();
        }
        this.scanningThreads.clear();
    }

    public void refreshDatabase() {
        if(this.refreshing) return;
        this.refreshing = true;
        this.scheduler.execute(() -> {
            List<String> servers = ServerManager.getAllAddress();
            for(String server : servers) {
                this.refresh(server);
            }
            this.refreshing = false;
        });
    }

    public void refresh(String address) {
        LOGGER.info("Refreshing server " + address);
        MCPingOptions options = MCPingOptions.builder()
                .hostname(address.split(":")[0])
                .port(Integer.parseInt(address.split(":")[1]))
                .build();

        MCPingResponse data = null;

        try {
            data = MCPing.getPing(options);
        } catch (Exception e) {
            LOGGER.error("Server " + address + " is offline !");
        }

        Optional<Server> optionalServer = ServerManager.getServerByAddress(address);
        Server server = optionalServer.orElse(new Server());
        server.setAddress(address);

        if(data != null) {
            server.setDescription(Base64.getEncoder().encodeToString(data.getDescription().getText().replaceAll(MC_COLOR_REGEX, "").getBytes(StandardCharsets.UTF_8)));
            server.setVersion(data.getVersion().getName());
            server.setOnlinePlayers(data.getPlayers().getOnline());
            server.setMaxPlayers(data.getPlayers().getMax());
            if(data.getPlayers().getOnline() > 0 && data.getPlayers().getSample().size() > 0) server.setPlayers(data.getPlayers().getSample().stream().map(plr -> plr.getName().replaceAll(MC_COLOR_REGEX, "")).collect(Collectors.joining(";")));
            server.setModLoader("VANILLA");
            server.setOnline(true);
            server.setLastOnline(System.currentTimeMillis());
        } else if(optionalServer.isPresent()) {
            server.setOnline(false);
            server.setLastOnline(System.currentTimeMillis());
        }

        if(optionalServer.isEmpty()) {
            this.jda.getTextChannelById("909273013898346516").sendMessageEmbeds(this.generateServerEmbed("New server found !", server)).queue();
        }

        if(data != null || optionalServer.isPresent()) {
            DatabaseManager.save(server);
        }
    }

    public MessageEmbed generateServerEmbed(String title, Server server) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(title, null, this.jda.getSelfUser().getAvatarUrl());
        builder.addField("Address:", server.getAddress(), true);
        builder.addField("Version:", server.getVersion(), true);
        builder.addField("ModLoader:", "VANILLA", true);
        builder.addField("Players:", server.getOnlinePlayers() + "/" + server.getMaxPlayers(), true);
        if(!server.getDescription().isEmpty()) builder.addField("Description:", "```" + server.getDescription() + "```", false);
        String[] players = server.getPlayers().split(";");
        if(server.getOnlinePlayers() > 0 && players.length > 0) builder.addField("Online Players:", "```" + String.join("\n", players) + "```", false);
        builder.setColor(new Color(52, 152, 219));

        return builder.build();
    }

    private void scan(String portRange) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("masscan", "--excludefile", "exclude.conf", "-p" + portRange, "--rate", BotConfig.get("SCAN_RATE"), "0.0.0.0/0");

        try {
            p = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while((line = reader.readLine()) != null) {
                if(line.startsWith("Discovered open port")) {
                    String[] split = line.split(" ");
                    String ip = split[5];
                    String port = split[3].replace("/tcp", "");
                    this.scanningQueue.add(ip + ":" + port);
                }
            }

            LOGGER.info("Scan finished !");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCooldown(User user, String command, long cooldown){
        if(!this.cooldowns.containsKey(user.getId())){
            this.cooldowns.put(user.getId(), new HashMap<>());
        }

        this.cooldowns.get(user.getId()).put(command, System.currentTimeMillis() + cooldown);
    }

    public long getRemainingCooldown(User user, String command){
        if(!this.cooldowns.containsKey(user.getId())){
            return 0;
        }

        return this.cooldowns.get(user.getId()).getOrDefault(command, 0L) - System.currentTimeMillis();
    }

}
