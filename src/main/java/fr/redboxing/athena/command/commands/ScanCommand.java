package fr.redboxing.athena.command.commands;

import fr.redboxing.athena.AthenaBot;
import fr.redboxing.athena.command.AbstractCommand;
import fr.redboxing.athena.command.CommandCategory;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class ScanCommand extends AbstractCommand {
    public ScanCommand(AthenaBot bot) {
        super(bot);

        this.name = "scan";
        this.category = CommandCategory.MISCS;
        this.help = "Start a scan if there is no pending scan.";
        this.options.add(new OptionData(OptionType.STRING, "port", "The port range to scan", true));
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event) {
        if(this.bot.start(event.getOption("port").getAsString())) {
            event.reply("Scan started.").queue();
        } else {
            event.reply("There is already a scan in progress.").queue();
        }
    }
}
