package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.commands.suggestions.ListSuggestion;
import com.adibarra.utils.ADText;

/** `/et config reset <key|all>` */
public class ResetCommand implements Command<ServerCommandSource> {

    // "all" plus every non-reserved key (reserved keys are managed internally)
    public static final SuggestionProvider<ServerCommandSource> KEY_SUGGESTIONS = ListSuggestion.of(() -> {
        List<String> options = new ArrayList<>();
        options.add("all");
        for (String key : ETMixinPlugin.getConfig().getKeys()) {
            if (!ETConfigSchema.isReserved(key))
                options.add(key);
        }
        return options;
    });

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = getOptionalString(context, "key");

        if (key == null) {
            CommandFeedback.feedback(source, Text.literal("Usage: "),
                Text.literal("/et config reset ").formatted(Formatting.AQUA),
                Text.literal("<key|all>").formatted(Formatting.RED));
            return Command.SINGLE_SUCCESS;
        }

        key = key.toLowerCase(Locale.ROOT);
        return key.equals("all") ? resetAll(source) : resetKey(source, key);
    }

    private int resetKey(ServerCommandSource source, String key) {
        if (ETMixinPlugin.getConfig().getOrDefault(key, null) == null) {
            CommandFeedback.error(source, Text.literal("Key '").formatted(Formatting.GRAY),
                Text.literal(key).formatted(Formatting.RED),
                Text.literal("' does not exist.").formatted(Formatting.GRAY));
            return 0;
        }

        if (ETConfigSchema.isReserved(key)) {
            CommandFeedback.error(source, Text.literal("Key '").formatted(Formatting.GRAY),
                Text.literal(key).formatted(Formatting.RED),
                Text.literal("' is reserved and cannot be reset.").formatted(Formatting.GRAY));
            return 0;
        }

        String previous = ETMixinPlugin.getConfig().getOrDefault(key, null);
        String value = ETConfigSchema.defaultOf(key);
        if (!ETMixinPlugin.getConfig().set(key, value)) {
            CommandFeedback.error(source, Text.literal("Failed to persist reset for key '").formatted(Formatting.GRAY),
                Text.literal(key).formatted(Formatting.RED),
                Text.literal("'. The value was not changed.").formatted(Formatting.GRAY));
            return 0;
        }
        ETMixinPlugin.clearCaches();
        ETCommands.broadcastConfigSync(source.getServer());

        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal("Key '").formatted(Formatting.GRAY));
        msg.add(Text.literal(key));
        msg.add(Text.literal("' reset to '").formatted(Formatting.GRAY));
        msg.add(ADText.colorValue(value));
        msg.add(Text.literal("'.").formatted(Formatting.GRAY));
        if (key.equals("mod_enabled") && !value.equals(previous)) {
            String isServer = source.getServer().isDedicated() ? "server " : "";
            msg.add(Text.literal("\nThis change requires a " + isServer + "restart to take effect.")
                .formatted(Formatting.LIGHT_PURPLE));
        }
        CommandFeedback.feedback(source, msg);
        return Command.SINGLE_SUCCESS;
    }

    private int resetAll(ServerCommandSource source) {
        Map<String, String> defaults = ETConfigSchema.defaults();

        String prevModEnabled = ETMixinPlugin.getConfig().getOrDefault("mod_enabled", null);
        int restored = 0;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String current = ETMixinPlugin.getConfig().getOrDefault(entry.getKey(), null);
            if (!entry.getValue().equals(current))
                restored++;
        }

        if (!ETMixinPlugin.getConfig().setAllAndPersist(defaults)) {
            CommandFeedback.error(source, Text.literal("Failed to persist config reset.").formatted(Formatting.GRAY));
            return 0;
        }
        ETMixinPlugin.clearCaches();
        ETCommands.broadcastConfigSync(source.getServer());

        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal("Restored ").formatted(Formatting.GRAY));
        msg.add(Text.literal(String.valueOf(restored)).formatted(Formatting.AQUA));
        msg.add(Text.literal(" config key" + (restored == 1 ? "" : "s") + " to defaults.").formatted(Formatting.GRAY));
        String defModEnabled = defaults.get("mod_enabled");
        if (defModEnabled != null && !defModEnabled.equals(prevModEnabled)) {
            String isServer = source.getServer().isDedicated() ? "server " : "";
            msg.add(Text.literal("\nThis change requires a " + isServer + "restart to take effect.")
                .formatted(Formatting.LIGHT_PURPLE));
        }
        CommandFeedback.feedback(source, msg);
        return Command.SINGLE_SUCCESS;
    }

    private static String getOptionalString(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
