package com.adibarra.utils;

public class ADMisc {

    /**
     * Checks if a string is a boolean value.
     * @param value the string to check
     * @return true if the string is a boolean value, false otherwise
     */
    public static boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }

    /**
     * Checks if a string is an integer value.
     * @param value the string to check
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
     * Checks if a string is a double value.
     * @param value the string to check
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
