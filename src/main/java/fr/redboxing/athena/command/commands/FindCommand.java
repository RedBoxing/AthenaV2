package fr.redboxing.athena.command.commands;

import fr.redboxing.athena.AthenaBot;
import fr.redboxing.athena.command.AbstractCommand;
import fr.redboxing.athena.command.CommandCategory;
import fr.redboxing.athena.database.entities.Server;
import fr.redboxing.athena.manager.ServerManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Optional;

public class FindCommand extends AbstractCommand {
    public FindCommand(AthenaBot bot) {
        super(bot);

        this.name = "find";
        this.help = "Find a player is the server database";
        this.category = CommandCategory.MISCS;
        this.options.add(new OptionData(OptionType.STRING, "username", "The username of the player", true));
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();
        Optional<Server> server = ServerManager.getServerByPlayer(username);
        if(server.isPresent()) {
            event.replyEmbeds(this.bot.generateServerEmbed("Player " + username + " found !", server.get())).queue();
        } else {
            event.reply("Player " + username + " not found !").queue();
        }
    }
}
