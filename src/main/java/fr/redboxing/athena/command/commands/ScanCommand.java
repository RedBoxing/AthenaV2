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
        this.options.add(new OptionData(OptionType.STRING, "ip-range", "The range of the scan", false));
        this.options.add(new OptionData(OptionType.STRING, "port-range", "The port range to scan", false));
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> {
            if(this.bot.start(event.getOption("ip-range") == null ? "0.0.0.0/0" : event.getOption("ip-range").getAsString(), event.getOption("port-range") == null ? "0-65535" : event.getOption("port-range").getAsString())) {
                hook.editOriginal("Scan started.").queue();
            } else {
                hook.editOriginal("There is already a scan in progress.").queue();
            }
        });
    }
}
