package fr.redboxing.athena;

import fr.redboxing.athena.command.AbstractCommand;
import fr.redboxing.athena.manager.ServerManager;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class EventListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);
    private AthenaBot bot;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(event.getGuild() == null) return;

        AbstractCommand command = this.bot.getCommandManager().getCommand(event.getName());
        if(command != null) {
            command.run(event);
        } else {
            event.replyEmbeds(new EmbedBuilder()
                    .setAuthor("Unexpected error !", event.getJDA().getSelfUser().getAvatarUrl())
                    .setDescription("Command does not exist !")
                    .setColor(Color.RED)
                    .setFooter("RedBot by RedBoxing", event.getJDA().getUserById(BotConfig.getLong("AUTHOR_ID")).getAvatarUrl())
                    .build()
            ).queue();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        this.bot.getJda().updateCommands().addCommands(this.bot.getCommandManager().getCommands().values().stream().map(AbstractCommand::buildCommandData).toArray(CommandData[]::new)).queue(cmds -> {
            LOGGER.info("Registered {} commands !", cmds.size());
        });

        this.bot.getScheduler().scheduleAtFixedRate(() -> this.bot.refreshDatabase(), 0, 10, TimeUnit.MINUTES);
        this.bot.getScheduler().scheduleAtFixedRate(() -> this.bot.getJda().getPresence().setActivity(Activity.playing(ServerManager.getTotalServerCounts() + " scanned servers !")), 0, 10, TimeUnit.MINUTES);
    }
}
