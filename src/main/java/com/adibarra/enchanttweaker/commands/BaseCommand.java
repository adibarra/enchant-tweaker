package com.adibarra.enchanttweaker.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.utils.ADText;

public class BaseCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CommandFeedback.feedback(context.getSource(), Component.literal("Try running ").withStyle(ChatFormatting.GRAY),
            ADText.buildCmdLink(ETCommands.BASE_CMD, "help"),
            Component.literal(" for more information.").withStyle(ChatFormatting.GRAY));
        return Command.SINGLE_SUCCESS;
    }
}
