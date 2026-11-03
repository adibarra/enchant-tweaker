package com.adibarra.enchanttweaker.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ConfigCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CommandFeedback.feedback(context.getSource(), Component.literal("Usage:"),
            Component.literal("\n  /et config ").withStyle(ChatFormatting.AQUA),
            Component.literal("<key> ").withStyle(ChatFormatting.RED),
            Component.literal("[value]").withStyle(ChatFormatting.GRAY),
            Component.literal("\n  /et config list ").withStyle(ChatFormatting.AQUA),
            Component.literal("[category] [page]").withStyle(ChatFormatting.GRAY),
            Component.literal("\n  /et config reset ").withStyle(ChatFormatting.AQUA),
            Component.literal("<key|all>").withStyle(ChatFormatting.RED));
        return Command.SINGLE_SUCCESS;
    }
}
