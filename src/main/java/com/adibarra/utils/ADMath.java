package com.adibarra.utils;

@SuppressWarnings("unused")
public class ADMath {

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

    /**
     * Clamps a value between a minimum and maximum.
     * @return the clamped value
     */
    public static long clamp(long val, long min, long max) {
        return Math.max(min, Math.min(max, val));
    }
}
