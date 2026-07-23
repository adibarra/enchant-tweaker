package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADText;

public class SetCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key").toLowerCase(Locale.ROOT);
        // trim the remaining command text before validation
        String value = boolString(StringArgumentType.getString(context, "value").trim());
        ServerCommandSource source = context.getSource();

        // key must exist before we can set it
        if (ETMixinPlugin.getConfig().getOrDefault(key, null) == null) {
            CommandFeedback.error(source, Text.literal("Key '").formatted(Formatting.GRAY),
                Text.literal(key).formatted(Formatting.RED),
                Text.literal("' does not exist.").formatted(Formatting.GRAY));
            return 0;
        }

        // reserved config keys are managed internally and cannot be changed
        if (ETConfigSchema.isReserved(key)) {
            CommandFeedback.error(source, Text.literal("Key '").formatted(Formatting.GRAY),
                Text.literal(key).formatted(Formatting.RED),
                Text.literal("' is reserved and cannot be changed.").formatted(Formatting.GRAY));
            return 0;
        }

        // value must be valid for the key's type (unknown keys are allowed)
        if (!ETConfigSchema.isValid(key, value)) {
            CommandFeedback.error(source, Text.literal("Value '").formatted(Formatting.GRAY),
                Text.literal(value).formatted(Formatting.RED),
                Text.literal("' is not valid for '").formatted(Formatting.GRAY), Text.literal(key),
                Text.literal("'. Expected ").formatted(Formatting.GRAY),
                Text.literal(ETConfigSchema.expected(key)).formatted(Formatting.AQUA),
                Text.literal(".").formatted(Formatting.GRAY));
            return 0;
        }

        // update memory and disk, then clear the config cache
        if (!ETMixinPlugin.getConfig().set(key, value)) {
            CommandFeedback.error(source, Text.literal("Failed to persist key '").formatted(Formatting.GRAY),
                Text.literal(key).formatted(Formatting.RED),
                Text.literal("'. The value was not changed.").formatted(Formatting.GRAY));
            return 0;
        }
        ETMixinPlugin.clearCaches();
        ETCommands.broadcastConfigSync(source.getServer());

        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal("Key '").formatted(Formatting.GRAY));
        msg.add(Text.literal(key));
        msg.add(Text.literal("' set to '").formatted(Formatting.GRAY));
        msg.add(ADText.colorValue(value));
        msg.add(Text.literal("'.").formatted(Formatting.GRAY));
        if (key.equals("mod_enabled") && (value.equals("true") || value.equals("false"))) {
            String isServer = source.getServer().isDedicated() ? "server " : "";
            msg.add(Text.literal("\nThis change requires a " + isServer + "restart to take effect.")
                .formatted(Formatting.LIGHT_PURPLE));
        }
        CommandFeedback.feedback(source, msg);
        return Command.SINGLE_SUCCESS;
    }

    private static String boolString(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (ADText.TRUE_VALUES.contains(normalized))
            return "true";
        if (ADText.FALSE_VALUES.contains(normalized))
            return "false";
        return value;
    }
}
