package com.adibarra.utils;

@SuppressWarnings("unused")
public class ADMisc {

    private ADMisc() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * checks if a string is a boolean value
     *
     * @param value
     * @return true if the string is a boolean value, false otherwise
     */
    public static boolean isBoolean(String value) {
        return value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"));
    }

    /**
     * checks if a string is an integer value
     *
     * @param value
     * @return true if the string is an integer value, false otherwise
     */
    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * checks if a string is a double value
     *
     * @param value
     * @return true if the string is a double value, false otherwise
     */
    public static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
