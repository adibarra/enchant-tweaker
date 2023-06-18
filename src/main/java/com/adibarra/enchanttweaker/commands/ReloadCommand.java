package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements Command<ServerCommandSource> {

    // VERSION CHANGES:
    // 1.16+: sendFeedback(Text)
    // 1.20+: sendFeedback(() -> Text)
    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        ETMixinPlugin.reloadConfig();

        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        msg.add(Text.literal("Config Reloaded!"));

        context.getSource().sendFeedback(() -> ADText.joinText(msg), false);
        return Command.SINGLE_SUCCESS;
    }
}
