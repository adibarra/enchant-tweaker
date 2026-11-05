package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADText;

public class SetCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key").toLowerCase(Locale.ROOT);
        // trim the remaining command text before validation
        String value = boolString(StringArgumentType.getString(context, "value").trim());
        CommandSourceStack source = context.getSource();

        // key must exist before we can set it
        if (ETMixinPlugin.getConfig().getOrDefault(key, null) == null) {
            CommandFeedback.error(source, Component.literal("Key '").withStyle(ChatFormatting.GRAY),
                Component.literal(key).withStyle(ChatFormatting.RED),
                Component.literal("' does not exist.").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        // reserved config keys are managed internally and cannot be changed
        if (ETConfigSchema.isReserved(key)) {
            CommandFeedback.error(source, Component.literal("Key '").withStyle(ChatFormatting.GRAY),
                Component.literal(key).withStyle(ChatFormatting.RED),
                Component.literal("' is reserved and cannot be changed.").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        // value must be valid for the key's type (unknown keys are allowed)
        if (!ETConfigSchema.isValid(key, value)) {
            CommandFeedback.error(source, Component.literal("Value '").withStyle(ChatFormatting.GRAY),
                Component.literal(value).withStyle(ChatFormatting.RED),
                Component.literal("' is not valid for '").withStyle(ChatFormatting.GRAY), Component.literal(key),
                Component.literal("'. Expected ").withStyle(ChatFormatting.GRAY),
                Component.literal(ETConfigSchema.expected(key)).withStyle(ChatFormatting.AQUA),
                Component.literal(".").withStyle(ChatFormatting.GRAY));
            return 0;
        }

        // update memory and disk, then clear the config cache
        if (!ETMixinPlugin.getConfig().set(key, value)) {
            CommandFeedback.error(source, Component.literal("Failed to persist key '").withStyle(ChatFormatting.GRAY),
                Component.literal(key).withStyle(ChatFormatting.RED),
                Component.literal("'. The value was not changed.").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        ETMixinPlugin.clearCaches();
        ETCommands.broadcastConfigSync(source.getServer());

        List<Component> msg = new ArrayList<>();
        msg.add(Component.literal("Key '").withStyle(ChatFormatting.GRAY));
        msg.add(Component.literal(key));
        msg.add(Component.literal("' set to '").withStyle(ChatFormatting.GRAY));
        msg.add(ADText.colorValue(value));
        msg.add(Component.literal("'.").withStyle(ChatFormatting.GRAY));
        if (key.equals("mod_enabled") && (value.equals("true") || value.equals("false"))) {
            String isServer = source.getServer().isDedicatedServer() ? "server " : "";
            msg.add(Component.literal("\nThis change requires a " + isServer + "restart to take effect.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
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
