package com.adibarra.enchanttweaker.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.utils.ADText;

public class BaseCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        CommandFeedback.feedback(context.getSource(), Text.literal("Try running ").formatted(Formatting.GRAY),
            ADText.buildCmdLink(ETCommands.BASE_CMD, "help"),
            Text.literal(" for more information.").formatted(Formatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }
}
