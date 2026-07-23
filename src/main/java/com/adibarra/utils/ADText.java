package com.adibarra.utils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

@SuppressWarnings("unused")
public class ADText {

    private ADText() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    public static final Set<String> TRUE_VALUES = Set.of("true", "t", "yes", "on", "enable", "enabled");
    public static final Set<String> FALSE_VALUES = Set.of("false", "f", "no", "off", "disable", "disabled");

    /**
     * joins a list of text objects into a single mutable text object
     *
     * @param list
     * @return the joined mutable text object
     */
    public static MutableText joinTextMutable(List<Text> list) {
        MutableText out = Text.empty();
        for (Text text : list) {
            out.append(text);
        }
        return out;
    }

    /**
     * joins a list of text objects into a single text object
     *
     * @param list
     * @return the joined text object
     */
    public static Text joinText(List<Text> list) {
        return joinTextMutable(list);
    }

    /**
     * builds a pretty command link with hover and click events
     *
     * @param literal
     * @return the built command link as a mutable text object
     */
    public static MutableText buildCmdLink(String base, String literal) {
        String cmd = "/" + base + " " + literal;
        return Text.literal(cmd)
            .setStyle(Style.EMPTY.withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to run " + cmd))));
    }

    /**
     * colors the value based on its type
     *
     * @param value
     * @return the colored value as a mutable text object
     */
    public static MutableText colorValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(normalized)) {
            return Text.literal(value).formatted(Formatting.GREEN);
        } else if (FALSE_VALUES.contains(normalized)) {
            return Text.literal(value).formatted(Formatting.RED);
        } else if (ADMisc.isDouble(normalized)) {
            return Text.literal(value).formatted(Formatting.BLUE);
        }
        return Text.literal(value).formatted(Formatting.GOLD);
    }
}
