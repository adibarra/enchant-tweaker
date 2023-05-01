package com.adibarra.utils;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.List;

@SuppressWarnings("unused")
public class ADText {

	/**
	 * Joins an array of Text objects into a single MutableText object.
	 * @param array the array of Text objects to join
	 * @return the joined MutableText object
	 */
	public static MutableText joinText(Text[] array) {
		MutableText out = Text.empty();
		for (Text text : array) {
			out.append(text);
		}
		return out;
	}

	/**
	 * Joins an array of Text objects into a single MutableText object.
	 * @param array the array of Text objects to join
	 * @return the joined MutableText object
	 */
	public static MutableText joinText(List<Text> array) {
		MutableText out = Text.empty();
		for (Text text : array) {
			out.append(text);
		}
		return out;
	}

    /**
     * Builds a pretty command link with hover and click events.
     * @param literal the literal to run
     * @return the built command link as a MutableText object
     */
    public static MutableText buildCmdLink(String literal) {
        return Text.literal(literal)
            .setStyle(Style.EMPTY
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, literal))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to run " + literal)))
            );
    }
}
