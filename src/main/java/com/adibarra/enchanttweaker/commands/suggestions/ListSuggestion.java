package com.adibarra.enchanttweaker.commands.suggestions;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

public class ListSuggestion {

    private ListSuggestion() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    private static CompletableFuture<Suggestions> buildSuggestions(SuggestionsBuilder builder,
        Collection<String> options) {
        String query = builder.getRemaining().toLowerCase(Locale.ROOT);

        if (options == null || options.isEmpty()) {
            return Suggestions.empty();
        }

        for (String str : options) {
            if (str != null && str.toLowerCase(Locale.ROOT).startsWith(query)) {
                builder.suggest(str);
            }
        }

        return builder.buildFuture();
    }

    public static SuggestionProvider<ServerCommandSource> of(Supplier<Collection<String>> options) {
        return (CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) -> buildSuggestions(builder,
            options == null ? null : options.get());
    }
}
