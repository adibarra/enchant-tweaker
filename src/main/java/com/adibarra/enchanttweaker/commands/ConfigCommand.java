package com.adibarra.enchanttweaker.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ConfigCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        CommandFeedback.feedback(context.getSource(),
            Text.literal("Usage:"),
            Text.literal("\n  /et config ").formatted(Formatting.AQUA),
            Text.literal("<key> ").formatted(Formatting.RED),
            Text.literal("[value]").formatted(Formatting.GRAY),
            Text.literal("\n  /et config list ").formatted(Formatting.AQUA),
            Text.literal("[category] [page]").formatted(Formatting.GRAY),
            Text.literal("\n  /et config reset ").formatted(Formatting.AQUA),
            Text.literal("<key|all>").formatted(Formatting.RED));
        return Command.SINGLE_SUCCESS;
    }
}
