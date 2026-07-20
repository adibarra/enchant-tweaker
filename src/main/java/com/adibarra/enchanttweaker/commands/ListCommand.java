package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.commands.suggestions.ListSuggestion;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** `/et config list [category] [page]` */
public class ListCommand implements Command<ServerCommandSource> {

    private static final int PAGE_SIZE = 15;

    // "all" plus every schema category slug
    public static final SuggestionProvider<ServerCommandSource> CATEGORY_SUGGESTIONS = ListSuggestion.of(() -> {
        List<String> options = new ArrayList<>();
        options.add("all");
        options.addAll(ETConfigSchema.categories());
        return options;
    });

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        String category = getOptionalString(context, "category");
        if (category == null) {
            return overview(context);
        }
        return listCategory(context, category.toLowerCase(), getOptionalInt(context, "page", 1));
    }

    private int overview(CommandContext<ServerCommandSource> context) {
        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal("Config categories:").formatted(Formatting.GRAY));
        for (String slug : ETConfigSchema.categories()) {
            int count = ETConfigSchema.keysIn(slug).size();
            msg.add(Text.literal("\n  "));
            msg.add(ADText.buildCmdLink(ETCommands.BASE_CMD, "config list " + slug));
            msg.add(Text.literal(" - " + count + " key" + (count == 1 ? "" : "s")).formatted(Formatting.GRAY));
        }
        CommandFeedback.feedback(context.getSource(), msg);
        return Command.SINGLE_SUCCESS;
    }

    private int listCategory(CommandContext<ServerCommandSource> context, String category, int page) {
        List<String> keys = new ArrayList<>();
        if (category.equals("all")) {
            for (String slug : ETConfigSchema.categories()) {
                keys.addAll(ETConfigSchema.keysIn(slug));
            }
        } else if (ETConfigSchema.categories().contains(category)) {
            keys.addAll(ETConfigSchema.keysIn(category));
        } else {
            CommandFeedback.error(context.getSource(),
                Text.literal("Unknown category '").formatted(Formatting.GRAY),
                Text.literal(category).formatted(Formatting.RED),
                Text.literal("'.").formatted(Formatting.GRAY));
            return 0;
        }

        int totalPages = Math.max(1, (keys.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 1 || page > totalPages) {
            CommandFeedback.error(context.getSource(),
                Text.literal("Page ").formatted(Formatting.GRAY),
                Text.literal(String.valueOf(page)).formatted(Formatting.RED),
                Text.literal(" is out of range (1-" + totalPages + ").").formatted(Formatting.GRAY));
            return 0;
        }

        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal(category).formatted(Formatting.AQUA));
        msg.add(Text.literal(" (page " + page + "/" + totalPages + ")").formatted(Formatting.GRAY));

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, keys.size());
        for (int i = start; i < end; i++) {
            String key = keys.get(i);
            String value = ETMixinPlugin.getConfig().getOrDefault(key, "");
            msg.add(Text.literal("\n  "));
            msg.add(Text.literal(key));
            msg.add(Text.literal(": ").formatted(Formatting.GRAY));
            if (value.isEmpty()) {
                msg.add(Text.literal("(empty)").formatted(Formatting.DARK_GRAY));
            } else {
                msg.add(ADText.colorValue(value));
            }
        }
        CommandFeedback.feedback(context.getSource(), msg);
        return Command.SINGLE_SUCCESS;
    }

    private static String getOptionalString(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int getOptionalInt(CommandContext<ServerCommandSource> ctx, String name, int def) {
        try {
            return IntegerArgumentType.getInteger(ctx, name);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
