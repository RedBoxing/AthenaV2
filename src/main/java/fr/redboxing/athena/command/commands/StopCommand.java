package fr.redboxing.athena.command.commands;

import fr.redboxing.athena.AthenaBot;
import fr.redboxing.athena.command.AbstractCommand;
import fr.redboxing.athena.command.CommandCategory;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class StopCommand extends AbstractCommand {
    public StopCommand(AthenaBot bot) {
        super(bot);

        this.name = "stop";
        this.help = "Stop the current scan.";
        this.category = CommandCategory.MISCS;
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event) {
        this.bot.stop();
        event.reply("Scan stopped.").queue();
    }
}
