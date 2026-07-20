package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;

/** small helpers for command output */
public final class CommandFeedback {

    private CommandFeedback() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * builds the mod prefix followed by the given parts, joined into a single Text
     */
    private static MutableText build(List<Text> parts) {
        List<Text> msg = new ArrayList<>();
        msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        msg.addAll(parts);
        return ADText.joinTextMutable(msg);
    }

    public static void feedback(ServerCommandSource source, List<Text> parts) {
        MutableText out = build(parts);
        source.sendFeedback(() -> out, false);
    }

    public static void feedback(ServerCommandSource source, Text... parts) {
        feedback(source, Arrays.asList(parts));
    }

    public static void error(ServerCommandSource source, List<Text> parts) {
        source.sendError(build(parts));
    }

    public static void error(ServerCommandSource source, Text... parts) {
        error(source, Arrays.asList(parts));
    }
}
