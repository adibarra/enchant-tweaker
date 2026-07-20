package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        List<Text> msg = new ArrayList<>();
        for (ADBrigadier.Command command : ETCommands.getCommands()) {
            msg.add(Text.literal("\n"));
            msg.add(ADText.buildCmdLink(ETCommands.BASE_CMD, command.node().get().getLiteral()));
            msg.add(Text.literal(" - " + command.description()));
        }

        msg.add(Text.literal("\n"));
        msg.add(ADText.buildCmdLink(ETCommands.BASE_CMD, "help"));
        msg.add(Text.literal(" - Shows this help message."));

        CommandFeedback.feedback(context.getSource(), msg);
        return Command.SINGLE_SUCCESS;
    }
}
