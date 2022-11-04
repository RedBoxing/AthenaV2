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

public class ServerCommand extends AbstractCommand {
    public ServerCommand(AthenaBot bot) {
        super(bot);

        this.name = "server";
        this.help = "View a server's informations";
        this.category = CommandCategory.MISCS;
        this.options.add(new OptionData(OptionType.STRING, "address", "The address in the format ip:port of the server", true));
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event) {
        String address = event.getOption("address").getAsString();
        event.deferReply().queue(hook -> {
            this.bot.refresh(address);
            List<Server> server = ServerManager.getServerByAddress(address);
            if(server.size() > 0) {
                hook.editOriginalEmbeds(this.bot.generateServerEmbed("Server informations", server.get(0))).queue();
            } else {
                hook.editOriginal("Server could not be found !").queue();
            }
        });
    }
}
