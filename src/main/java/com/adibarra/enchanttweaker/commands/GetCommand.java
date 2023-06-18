package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.commands.suggestions.ListSuggestion;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class GetCommand implements Command<ServerCommandSource> {

    public static final SuggestionProvider<ServerCommandSource> KEY_SUGGESTIONS = ListSuggestion.of(() -> ETMixinPlugin.getConfig().getKeys());

    // VERSION CHANGES:
    // 1.16+: sendFeedback(Text)
    // 1.20+: sendFeedback(() -> Text)
    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key").toLowerCase();
        String value = ETMixinPlugin.getConfig().getOrDefault(key, null);
        List<Text> msg = new ArrayList<>();

        msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        if (value != null) {
            msg.add(Text.literal("Key '").formatted(Formatting.GRAY));
            msg.add(Text.literal(key));
            msg.add(Text.literal("' is set to '").formatted(Formatting.GRAY));
            msg.add(ADText.colorValue(value.toLowerCase()));
            msg.add(Text.literal("'.").formatted(Formatting.GRAY));
            context.getSource().sendFeedback(() -> ADText.joinText(msg), false);
            return Command.SINGLE_SUCCESS;
        }

        msg.add(Text.literal("Key '").formatted(Formatting.GRAY));
        msg.add(Text.literal(key).formatted(Formatting.RED));
        msg.add(Text.literal("' does not exist.").formatted(Formatting.GRAY));
        context.getSource().sendError(ADText.joinText(msg));
        return 0;
    }
}
