package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADText;

public class HelpCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        List<Component> msg = new ArrayList<>();
        for (ADBrigadier.Command command : ETCommands.getCommands()) {
            msg.add(Component.literal("\n"));
            msg.add(ADText.buildCmdLink(ETCommands.BASE_CMD, command.node().get().getLiteral()));
            msg.add(Component.literal(" - " + command.description()));
        }

        msg.add(Component.literal("\n"));
        msg.add(ADText.buildCmdLink(ETCommands.BASE_CMD, "help"));
        msg.add(Component.literal(" - Shows this help message."));

        CommandFeedback.feedback(context.getSource(), msg);
        return Command.SINGLE_SUCCESS;
    }
}
