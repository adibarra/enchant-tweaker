package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.commands.suggestions.ListSuggestion;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GetCommand implements Command<ServerCommandSource> {

    // reserved keys are read-only from commands, so keep them out of tab-completion
    public static final SuggestionProvider<ServerCommandSource> KEY_SUGGESTIONS = ListSuggestion.of(() ->
        ETMixinPlugin.getConfig().getKeys().stream()
            .filter(key -> !ETConfigSchema.isReserved(key))
            .toList());

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key").toLowerCase();
        String value = ETMixinPlugin.getConfig().getOrDefault(key, null);

        if (value != null) {
            CommandFeedback.feedback(context.getSource(),
                Text.literal("Key '").formatted(Formatting.GRAY),
                Text.literal(key),
                Text.literal("' is set to '").formatted(Formatting.GRAY),
                ADText.colorValue(value.toLowerCase()),
                Text.literal("'.").formatted(Formatting.GRAY));
            return Command.SINGLE_SUCCESS;
        }

        CommandFeedback.error(context.getSource(),
            Text.literal("Key '").formatted(Formatting.GRAY),
            Text.literal(key).formatted(Formatting.RED),
            Text.literal("' does not exist.").formatted(Formatting.GRAY));
        return 0;
    }
}
