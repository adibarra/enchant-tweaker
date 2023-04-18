package com.adibarra.enchanttweaker.utils;

public class Utils {
	public static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(max, val));
	}
}
