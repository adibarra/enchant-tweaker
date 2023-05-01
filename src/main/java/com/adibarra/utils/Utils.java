package com.adibarra.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Utils {

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
	public static MutableText joinText(Supplier<ArrayList<Text>> array) {
		MutableText out = Text.empty();
		for (Text text : array.get()) {
			out.append(text);
		}
		return out;
	}

	/**
	 * Clamps a value between a minimum and maximum.
	 * @return the clamped value
	 */
	public static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(max, val));
	}

	/**
	 * Clamps a value between a minimum and maximum.
	 * @return the clamped value
	 */
	public static double clamp(double val, double min, double max) {
		return Math.max(min, Math.min(max, val));
	}

	/**
	 * Clamps a value between a minimum and maximum.
	 * @return the clamped value
	 */
	public static float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}
}
