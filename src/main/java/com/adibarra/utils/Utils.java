package com.adibarra.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

@SuppressWarnings("unused")
public class Utils {
	public static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(max, val));
	}

	public static double clamp(double val, double min, double max) {
		return Math.max(min, Math.min(max, val));
	}

	public static float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}

	public record Conflict(String reason, BooleanSupplier condition) { }

	public static MutableText joinText(Text[] array) {
		MutableText out = Text.empty();
		for (Text text : array) {
			out.append(text);
		}
		return out;
	}

}
