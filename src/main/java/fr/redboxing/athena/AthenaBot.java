package fr.redboxing.athena;

import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingOptions;
import br.com.azalim.mcserverping.MCPingResponse;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.WebServiceClient;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import fr.redboxing.athena.command.CommandManager;
import fr.redboxing.athena.database.DatabaseManager;
import fr.redboxing.athena.database.entities.Server;
import fr.redboxing.athena.manager.ServerManager;
import fr.redboxing.athena.utils.ThreadFactoryHelper;
import fr.redboxing.athena.utils.Tuple;
import io.prometheus.client.exporter.HTTPServer;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
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

    private final Metrics metrics;

    private final DatabaseReader geoIPClient;

    public static void main(String[] args) {
        instance = new AthenaBot();
    }

    public AthenaBot() {
        this.scheduler = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() / 2, new ThreadFactoryHelper());

        this.commandManager = new CommandManager();
        this.commandManager.loadCommands(this);

        JDABuilder builder = JDABuilder.createDefault(BotConfig.get("BOT_TOKEN"));
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        builder.setBulkDeleteSplittingEnabled(true);
        builder.setCompression(Compression.NONE);
        builder.setActivity(Activity.playing(ServerManager.getTotalServerCounts() + " server scanned !"));
        builder.addEventListeners(new EventListener(this));

        this.jda = builder.build();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        this.metrics = new Metrics();
        this.metrics.updateAllMetrics();

        try {
            this.geoIPClient = new DatabaseReader.Builder(getClass().getResourceAsStream("/GeoLite2-City.mmdb")).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            HTTPServer server = new HTTPServer.Builder()
                    .withPort(Integer.parseInt(BotConfig.get("METRICS_PORT")))
                    .build();

            LOGGER.info("Metrics server started on port " + server.getPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean start(String ipRange, String portRange) {
        if(this.p != null) return false;
        Thread scan = new Thread(() -> this.scan(ipRange, portRange));
        scanningThreads.add(scan);
        scan.start();

        for(int i = 0; i < Runtime.getRuntime().availableProcessors() / 2; i++) {
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
        List<Tuple<String, Integer>> servers = ServerManager.getAllAddress();
        for(Tuple<String, Integer> tuple : servers) {
            try {
                this.refresh(tuple.getFirst() + ":" + tuple.getSecond());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        this.refreshing = false;
    }

    public void refresh(String address) {
        LOGGER.info("Refreshing server " + address);
        MCPingOptions options = MCPingOptions.builder()
                .hostname(address.split(":")[0])
                .port(Integer.parseInt(address.split(":")[1]))
                .build();

        MCPingResponse data = null;

        TextChannel channel = this.jda.getTextChannelById(BotConfig.get("CHANNEL_ID"));
        if(channel == null) {
            LOGGER.error("LOG CHANNEL NOT FOUND !");
            return;
        }

        try {
            data = MCPing.getPing(options);
        } catch (Exception e) {
            LOGGER.error("Server " + address + " is offline !");
        }

        List<Server> servers = ServerManager.getServerByAddress(address);
        Server server = servers.size() == 0 ? new Server() : servers.get(0);
        server.setHost(address.split(":")[0]);
        server.setPort(Integer.parseInt(address.split(":")[1]));

        if(data != null) {
            server.setDescription(/*Base64.getEncoder().encodeToString(data.getDescription().getText().replaceAll(MC_COLOR_REGEX, "").getBytes(StandardCharsets.UTF_8))*/ data.getDescription().getText().replaceAll(MC_COLOR_REGEX, ""));

            String[] version_splitted = data.getVersion().getName().split(" ");
            server.setVersion(version_splitted.length > 1 ? version_splitted[version_splitted.length - 1] : version_splitted[0]);
            server.setModloader(version_splitted.length > 1 ? version_splitted[0].toUpperCase() : "VANILLA");

            server.setOnlinePlayers(data.getPlayers().getOnline());
            server.setMaxPlayers(data.getPlayers().getMax());
            if(data.getPlayers().getOnline() > 0 && data.getPlayers().getSample() != null && data.getPlayers().getSample().size() > 0) {
                server.setPlayers(data.getPlayers().getSample().stream().map(plr -> plr.getName().replaceAll(MC_COLOR_REGEX, "")).collect(Collectors.joining(";")));
            } else {
                server.setPlayers("");
            }

            try {
                InetAddress ip = InetAddress.getByName(server.getHost());
                CityResponse city = this.geoIPClient.city(ip);

                server.setCity(city.getCity().getName() == null ? "Unknown" : city.getCity().getName());
                server.setLatitude(city.getLocation().getLatitude() == null ? 0 : city.getLocation().getLatitude());
                server.setLongitude(city.getLocation().getLongitude() == null ? 0 : city.getLocation().getLongitude());
            } catch (IOException | GeoIp2Exception e) {
                throw new RuntimeException(e);
            }

            server.setOnline(true);
            server.setLastOnline(System.currentTimeMillis());
        } else if(servers.size() > 0 && server.isOnline()) {
            server.setOnline(false);
        }

        if(servers.size() == 0 && data != null) {
            channel.sendMessageEmbeds(this.generateServerEmbed("New server found !", server)).queue();
            this.metrics.updateMetrics(null, server, true);
        } else if(servers.size() > 0 && server.isOnline() && data == null) {
            channel.sendMessageEmbeds(this.generateServerEmbed("Server went offline", server, Color.RED)).queue();
            this.metrics.updateMetrics(servers.get(0), server, false);
        } else if(servers.size() > 0 && data != null && !servers.get(0).isOnline()) {
            channel.sendMessageEmbeds(this.generateServerEmbed("Server went online !", server, new Color(12, 232, 81))).queue();
            this.metrics.updateMetrics(servers.get(0), server, false);
        }

        if(data != null || servers.size() > 0) {
            ServerManager.createOrUpdate(server);
        }
    }

    public MessageEmbed generateServerEmbed(String title, Server server) {
        return generateServerEmbed(title, server, null);
    }

    public MessageEmbed generateServerEmbed(String title, Server server, Color color) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(title, null, this.jda.getSelfUser().getAvatarUrl());
        builder.addField("Address:", server.getHost() + ":" + server.getPort(), true);
        builder.addField(server.isOnline() ? "Version:" : "Last Version:", server.getVersion(), true);
        builder.addField(server.isOnline() ? "ModLoader:" : "Last ModLoader", "VANILLA", true);

        if(!server.isOnline()) {
            builder.addField("Last Online: ", new SimpleDateFormat("dd/mm/yyyy HH:mm").format(server.getLastOnline()), true);
        }

        builder.addField(server.isOnline() ? "Players:" : "Last Players:", server.getOnlinePlayers() + "/" + server.getMaxPlayers(), true);
        if(!server.getDescription().isEmpty()) builder.addField(server.isOnline() ? "Description:" : "Last Description", "```" + /*new String(Base64.getDecoder().decode(server.getDescription()))*/ server.getDescription() + "```", false);
        if(server.getOnlinePlayers() > 0) {
            String[] players = server.getPlayers().split(";");
            if(players.length > 0) builder.addField(server.isOnline() ? "Online Players:" : "Last Online Players:", "```" + String.join("\n", players) + "```", false);
        }
        builder.setColor(color == null ? new Color(52, 152, 219) : color);

        return builder.build();
    }

    private void scan(String ipRange, String portRange) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(BotConfig.get("MASSCAN_EXECUTABLE"), "--excludefile", "exclude.conf", "-p" + portRange, "--rate", BotConfig.get("SCAN_RATE"), ipRange);

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
            this.stop();
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
