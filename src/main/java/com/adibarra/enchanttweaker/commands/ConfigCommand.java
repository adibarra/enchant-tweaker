package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class ConfigCommand implements Command<ServerCommandSource> {

    // VERSION CHANGES:
    // 1.19+: sendFeedback(
    // 1.20+: sendFeedback(() ->
    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        msg.add(Text.literal("Usage: /et config <key> [value]"));

        context.getSource().sendFeedback(() -> ADText.joinText(msg), false);
        return Command.SINGLE_SUCCESS;
    }
}
