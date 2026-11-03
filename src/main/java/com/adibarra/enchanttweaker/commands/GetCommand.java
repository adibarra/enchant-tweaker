package com.adibarra.enchanttweaker.commands;

import java.util.Locale;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.commands.suggestions.ListSuggestion;
import com.adibarra.utils.ADText;

public class GetCommand implements Command<CommandSourceStack> {

    // reserved keys are read-only from commands, so keep them out of tab-completion
    public static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS = ListSuggestion
        .of(() -> ETMixinPlugin.getConfig().getKeys().stream().filter(key -> !ETConfigSchema.isReserved(key)).toList());

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key").toLowerCase(Locale.ROOT);
        String value = ETMixinPlugin.getConfig().getOrDefault(key, null);

        if (value != null) {
            CommandFeedback.feedback(context.getSource(), Component.literal("Key '").withStyle(ChatFormatting.GRAY),
                Component.literal(key), Component.literal("' is set to '").withStyle(ChatFormatting.GRAY),
                ADText.colorValue(value), Component.literal("'.").withStyle(ChatFormatting.GRAY));
            return Command.SINGLE_SUCCESS;
        }

        CommandFeedback.error(context.getSource(), Component.literal("Key '").withStyle(ChatFormatting.GRAY),
            Component.literal(key).withStyle(ChatFormatting.RED),
            Component.literal("' does not exist.").withStyle(ChatFormatting.GRAY));
        return 0;
    }
}
