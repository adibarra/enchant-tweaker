package com.adibarra.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

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
	 * Reads given resource file as a string.
	 * @param fileName path to the resource file
	 * @return the file's contents
	 * @throws IOException if read fails for any reason
	 * @author <a href="https://stackoverflow.com/a/46613809">Lucio Paiva</a>
	 */
	public static String getResourceFileAsString(String fileName) throws IOException {
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		try (InputStream is = classLoader.getResourceAsStream(fileName)) {
			if (is == null) return null;
			try (InputStreamReader isr = new InputStreamReader(is);
				 BufferedReader reader = new BufferedReader(isr)) {
				return reader.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		}
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
