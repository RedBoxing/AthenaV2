package fr.redboxing.athena.command.commands;

import fr.redboxing.athena.AthenaBot;
import fr.redboxing.athena.command.AbstractCommand;
import fr.redboxing.athena.command.CommandCategory;
import fr.redboxing.athena.database.entities.Server;
import fr.redboxing.athena.manager.ServerManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Optional;

public class FindCommand extends AbstractCommand {
    public FindCommand(AthenaBot bot) {
        super(bot);

        this.name = "find";
        this.help = "Find a server database";
        this.category = CommandCategory.MISCS;
        this.options.add(new OptionData(OptionType.STRING, "target", "What to search for", true)
                .addChoice("IP (ip:port)", "ip")
                .addChoice("Description (contains)", "description")
                .addChoice("Player (username or UUID)", "player")
        );

        this.options.add(new OptionData(OptionType.STRING, "value", "The value to search", true));
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event) {
        switch (event.getOption("target").getAsString()) {
            case "ip" -> {
                List<Server> servers = ServerManager.getServerByAddress(event.getOption("value").getAsString());
                if(servers.size() > 0) {
                    event.reply("Found " + servers.size() + " server(s) with the IP " + event.getOption("value").getAsString()).queue(hook -> {
                        hook.editOriginalEmbeds(servers.stream().map(server -> this.bot.generateServerEmbed("Found server : " + server.getHost() + ":" + server.getPort(), server)).toList()).queue();
                    });
                } else {
                    event.reply("Server not found.").queue();
                }
            }
            case "description" -> {
                List<Server> servers = ServerManager.getServerByDescription(event.getOption("value").getAsString());
                if(servers.size() > 0) {
                    event.reply("Found " + servers.size() + " server(s) with the IP " + event.getOption("value").getAsString()).queue(hook -> {
                        hook.editOriginalEmbeds(servers.stream().map(server -> this.bot.generateServerEmbed("Found server : " + server.getHost() + ":" + server.getPort(), server)).toList()).queue();
                    });
                } else {
                    event.reply("Server not found.").queue();
                }
            }
            case "player" -> {
                List<Server> servers = ServerManager.getServerByPlayer(event.getOption("value").getAsString());
                if(servers.size() > 0) {
                    event.reply("Found " + servers.size() + " server(s) with the IP " + event.getOption("value").getAsString()).queue(hook -> {
                        hook.editOriginalEmbeds(servers.stream().map(server -> this.bot.generateServerEmbed("Found player : " + event.getOption("value").getAsString(), server)).toList()).queue();
                    });
                } else {
                    event.reply("Player not found.").queue();
                }
            }
        }
    }
}
