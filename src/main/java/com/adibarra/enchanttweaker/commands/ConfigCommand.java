package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ConfigCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(ADText.joinText(new Text[] {
            Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN),
            Text.literal("Usage: /et config <key> [value]")
        }), false);
        return Command.SINGLE_SUCCESS;
    }
}
