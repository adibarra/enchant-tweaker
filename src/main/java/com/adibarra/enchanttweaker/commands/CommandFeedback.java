package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;

/** small helpers for command output */
public final class CommandFeedback {

    private CommandFeedback() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * builds the mod prefix and joins text parts
     */
    private static MutableComponent build(List<Component> parts) {
        List<Component> msg = new ArrayList<>();
        msg.add(Component.literal(EnchantTweaker.PREFIX).withStyle(ChatFormatting.GREEN));
        msg.addAll(parts);
        return ADText.joinTextMutable(msg);
    }

    public static void feedback(CommandSourceStack source, List<Component> parts) {
        MutableComponent out = build(parts);
        source.sendSuccess(() -> out, false);
    }

    public static void feedback(CommandSourceStack source, Component... parts) {
        feedback(source, Arrays.asList(parts));
    }

    public static void error(CommandSourceStack source, List<Component> parts) {
        source.sendFailure(build(parts));
    }

    public static void error(CommandSourceStack source, Component... parts) {
        error(source, Arrays.asList(parts));
    }
}
