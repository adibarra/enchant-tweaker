package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ReloadCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ETMixinPlugin.reloadConfig();

        Text[] out = new Text[] {
            Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN),
            Text.literal("Config Reloaded!")
        };

        context.getSource().sendFeedback(ADText.joinText(out), false);
        return Command.SINGLE_SUCCESS;
    }
}
