package com.adibarra.enchanttweaker;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

public class ETUtils {
	public static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(max, val));
	}

	public static double clamp(double val, double min, double max) {
		return Math.max(min, Math.min(max, val));
	}

	public record Conflict(BooleanSupplier condition, String reason) { }

	public static MutableText joinText(Text[] array) {
		MutableText out = Text.empty();
		for (Text text : array) {
			out.append(text);
		}
		return out;
	}

}
