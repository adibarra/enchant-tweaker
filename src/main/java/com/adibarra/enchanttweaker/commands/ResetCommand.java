package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.commands.suggestions.ListSuggestion;
import com.adibarra.utils.ADText;

/** `/et config reset <key|all>` */
public class ResetCommand implements Command<CommandSourceStack> {

    // "all" plus every non-reserved key (reserved keys are managed internally)
    public static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS = ListSuggestion.of(() -> {
        List<String> options = new ArrayList<>();
        options.add("all");
        for (String key : ETMixinPlugin.getConfig().getKeys()) {
            if (!ETConfigSchema.isReserved(key))
                options.add(key);
        }
        return options;
    });

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String key = getOptionalString(context, "key");

        if (key == null) {
            CommandFeedback.feedback(source, Component.literal("Usage: "),
                Component.literal("/et config reset ").withStyle(ChatFormatting.AQUA),
                Component.literal("<key|all>").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }

        key = key.toLowerCase(Locale.ROOT);
        return key.equals("all") ? resetAll(source) : resetKey(source, key);
    }

    private int resetKey(CommandSourceStack source, String key) {
        if (ETMixinPlugin.getConfig().getOrDefault(key, null) == null) {
            CommandFeedback.error(source, Component.literal("Key '").withStyle(ChatFormatting.GRAY),
                Component.literal(key).withStyle(ChatFormatting.RED),
                Component.literal("' does not exist.").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        if (ETConfigSchema.isReserved(key)) {
            CommandFeedback.error(source, Component.literal("Key '").withStyle(ChatFormatting.GRAY),
                Component.literal(key).withStyle(ChatFormatting.RED),
                Component.literal("' is reserved and cannot be reset.").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        String previous = ETMixinPlugin.getConfig().getOrDefault(key, null);
        String value = ETConfigSchema.defaultOf(key);
        if (!ETMixinPlugin.getConfig().set(key, value)) {
            CommandFeedback.error(source,
                Component.literal("Failed to persist reset for key '").withStyle(ChatFormatting.GRAY),
                Component.literal(key).withStyle(ChatFormatting.RED),
                Component.literal("'. The value was not changed.").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        ETMixinPlugin.clearCaches();
        ETCommands.broadcastConfigSync(source.getServer());

        List<Component> msg = new ArrayList<>();
        msg.add(Component.literal("Key '").withStyle(ChatFormatting.GRAY));
        msg.add(Component.literal(key));
        msg.add(Component.literal("' reset to '").withStyle(ChatFormatting.GRAY));
        msg.add(ADText.colorValue(value));
        msg.add(Component.literal("'.").withStyle(ChatFormatting.GRAY));
        if (key.equals("mod_enabled") && !value.equals(previous)) {
            String isServer = source.getServer().isDedicatedServer() ? "server " : "";
            msg.add(Component.literal("\nThis change requires a " + isServer + "restart to take effect.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        CommandFeedback.feedback(source, msg);
        return Command.SINGLE_SUCCESS;
    }

    private int resetAll(CommandSourceStack source) {
        Map<String, String> defaults = ETConfigSchema.defaults();

        String prevModEnabled = ETMixinPlugin.getConfig().getOrDefault("mod_enabled", null);
        int restored = 0;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            String current = ETMixinPlugin.getConfig().getOrDefault(entry.getKey(), null);
            if (!entry.getValue().equals(current))
                restored++;
        }

        if (!ETMixinPlugin.getConfig().setAllAndPersist(defaults)) {
            CommandFeedback.error(source,
                Component.literal("Failed to persist config reset.").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        ETMixinPlugin.clearCaches();
        ETCommands.broadcastConfigSync(source.getServer());

        List<Component> msg = new ArrayList<>();
        msg.add(Component.literal("Restored ").withStyle(ChatFormatting.GRAY));
        msg.add(Component.literal(String.valueOf(restored)).withStyle(ChatFormatting.AQUA));
        msg.add(Component.literal(" config key" + (restored == 1 ? "" : "s") + " to defaults.")
            .withStyle(ChatFormatting.GRAY));
        String defModEnabled = defaults.get("mod_enabled");
        if (defModEnabled != null && !defModEnabled.equals(prevModEnabled)) {
            String isServer = source.getServer().isDedicatedServer() ? "server " : "";
            msg.add(Component.literal("\nThis change requires a " + isServer + "restart to take effect.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        CommandFeedback.feedback(source, msg);
        return Command.SINGLE_SUCCESS;
    }

    private static String getOptionalString(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
