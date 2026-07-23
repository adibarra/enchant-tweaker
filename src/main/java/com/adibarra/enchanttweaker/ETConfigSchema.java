package com.adibarra.enchanttweaker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * schema describing config value types from bundled defaults
 */
public final class ETConfigSchema {

    public enum ValueType {
        BOOLEAN, INTEGER, DECIMAL, LIST
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EnchantTweaker.MOD_NAME);
    private static final String DEFAULT_CONFIG_PATH = "assets/enchanttweaker/enchant-tweaker.properties";

    // preserve category and key order from the file
    private static final Map<String, ValueType> SCHEMA = new LinkedHashMap<>();
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_OF = new LinkedHashMap<>();
    private static final Map<String, List<String>> CATEGORY_KEYS = new LinkedHashMap<>();
    private static final Set<String> CATEGORY_ORDER = new LinkedHashSet<>();

    // hide reserved keys from config commands
    private static final Set<String> RESERVED = Set.of("config_version");

    private ETConfigSchema() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    static {
        try {
            String currentCategory = null;
            List<String> currentDescription = new ArrayList<>();
            boolean collectingDescription = false;
            for (String raw : loadDefaults()) {
                String trimmed = raw.trim();
                if (trimmed.isEmpty())
                    continue;
                if (isDescriptionBoundary(trimmed)) {
                    currentDescription.clear();
                    collectingDescription = false;
                    continue;
                }

                // detect categories before skipping comments
                String slug = parseCategoryHeader(trimmed);
                if (slug != null) {
                    currentCategory = slug;
                    CATEGORY_ORDER.add(slug);
                    CATEGORY_KEYS.computeIfAbsent(slug, k -> new ArrayList<>());
                    currentDescription.clear();
                    collectingDescription = true;
                    continue;
                }

                if (parseOptionHeader(trimmed) != null) {
                    currentDescription.clear();
                    collectingDescription = true;
                    continue;
                }

                if (trimmed.startsWith("#")) {
                    if (collectingDescription) {
                        String descriptionLine = commentBody(trimmed);
                        if (!descriptionLine.isEmpty())
                            currentDescription.add(descriptionLine);
                    }
                    continue;
                }

                String[] keyPair = trimmed.split("=", 2);
                if (keyPair.length != 2)
                    continue;

                String key = keyPair[0].trim().toLowerCase(Locale.ROOT);
                String value = keyPair[1].trim();
                String category = RESERVED.contains(key) ? "internal" : currentCategory;
                String previousCategory = CATEGORY_OF.put(key, category);
                if (previousCategory != null && !previousCategory.equals(category)) {
                    List<String> previousKeys = CATEGORY_KEYS.get(previousCategory);
                    if (previousKeys != null)
                        previousKeys.remove(key);
                }
                SCHEMA.put(key, inferType(value));
                DEFAULTS.put(key, value);
                DESCRIPTIONS.put(key, String.join(" ", currentDescription));
                if (category != null && !RESERVED.contains(key)) {
                    List<String> categoryKeys = CATEGORY_KEYS.get(category);
                    if (!categoryKeys.contains(key))
                        categoryKeys.add(key);
                }
            }
        } catch (Exception e) {
            // keep loading if the bundled schema cannot be read
            SCHEMA.clear();
            DEFAULTS.clear();
            DESCRIPTIONS.clear();
            CATEGORY_OF.clear();
            CATEGORY_KEYS.clear();
            CATEGORY_ORDER.clear();
            LOGGER.error(EnchantTweaker.PREFIX + "Failed to load config schema! Validation will be permissive.", e);
        }
    }

    /** detects a top-level category header */
    private static String parseCategoryHeader(String trimmed) {
        if (!trimmed.startsWith("###") || trimmed.startsWith("####"))
            return null;
        String rest = trimmed.substring(3).trim();
        if (rest.isEmpty() || rest.startsWith("#") || !rest.endsWith(":"))
            return null;
        return rest.substring(0, rest.length() - 1).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static String parseOptionHeader(String trimmed) {
        if (!trimmed.startsWith("###"))
            return null;
        String rest = trimmed.substring(3).trim();
        if (!rest.startsWith("##"))
            return null;
        int titleStart = 2;
        while (titleStart < rest.length() && rest.charAt(titleStart) == ' ')
            titleStart++;
        int spacing = titleStart - 2;
        if (spacing < 2 || spacing > 3 || titleStart == rest.length())
            return null;
        String title = rest.substring(titleStart).trim();
        return title.endsWith(":") ? title.substring(0, title.length() - 1).trim() : null;
    }

    private static boolean isDescriptionBoundary(String trimmed) {
        int hashes = 0;
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if (character == '#')
                hashes++;
            else if (!Character.isWhitespace(character))
                return false;
        }
        return hashes >= 8;
    }

    private static String commentBody(String line) {
        int index = 0;
        while (index < line.length() && (line.charAt(index) == '#' || Character.isWhitespace(line.charAt(index))))
            index++;
        return line.substring(index).trim();
    }

    /** loads the bundled default config lines from the classpath */
    private static List<String> loadDefaults() {
        try (InputStream is = ETConfigSchema.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (is == null)
                return Collections.emptyList();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read bundled config schema", e);
        }
    }

    /** infers the config value type */
    private static ValueType inferType(String value) {
        if (value.equals("true") || value.equals("false"))
            return ValueType.BOOLEAN;
        try {
            Integer.parseInt(value);
            return ValueType.INTEGER;
        } catch (NumberFormatException ignored) {
        }
        try {
            Double.parseDouble(value);
            return ValueType.DECIMAL;
        } catch (NumberFormatException ignored) {
        }
        return ValueType.LIST;
    }

    private static String normalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }

    /** gets the inferred type of a config key */
    public static ValueType typeOf(String key) {
        return SCHEMA.get(normalizeKey(key));
    }

    /** reports whether a key is reserved */
    public static boolean isReserved(String key) {
        String normalized = normalizeKey(key);
        return normalized != null && RESERVED.contains(normalized);
    }

    /** gets the bundled default value for a key */
    public static String defaultOf(String key) {
        return DEFAULTS.get(normalizeKey(key));
    }

    /** gets non-reserved defaults in file order */
    public static Map<String, String> defaults() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (!RESERVED.contains(entry.getKey()))
                out.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(out);
    }

    /** gets the bundled description associated with a config key */
    public static String descriptionOf(String key) {
        return DESCRIPTIONS.getOrDefault(normalizeKey(key), "");
    }

    /** gets the category slug a key belongs to */
    public static String categoryOf(String key) {
        return CATEGORY_OF.get(normalizeKey(key));
    }

    /**
     * gets the category slugs in file order, excluding categories that contain no
     * non-reserved keys
     */
    public static List<String> categories() {
        List<String> out = new ArrayList<>();
        for (String slug : CATEGORY_ORDER) {
            List<String> keys = CATEGORY_KEYS.get(slug);
            if (keys != null && !keys.isEmpty())
                out.add(slug);
        }
        return out;
    }

    /** gets the non-reserved keys in a category, in file order */
    public static List<String> keysIn(String categorySlug) {
        List<String> keys = CATEGORY_KEYS.get(normalizeKey(categorySlug));
        return keys == null ? Collections.emptyList() : new ArrayList<>(keys);
    }

    /** validates a value against its key's inferred type */
    public static boolean isValid(String key, String value) {
        ValueType type = SCHEMA.get(normalizeKey(key));
        if (type == null)
            return true;
        if (value == null)
            return false;
        return switch (type) {
            case BOOLEAN -> value.equals("true") || value.equals("false");
            case INTEGER -> {
                try {
                    Integer.parseInt(value);
                    yield true;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case DECIMAL -> {
                try {
                    yield Double.isFinite(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case LIST -> true;
        };
    }

    /** gets a human-readable hint describing the values accepted for a key */
    public static String expected(String key) {
        ValueType type = SCHEMA.get(normalizeKey(key));
        if (type == null)
            return "";
        return switch (type) {
            case BOOLEAN -> "true/false";
            case INTEGER -> "an integer";
            case DECIMAL -> "a number";
            case LIST -> "a comma-separated list";
        };
    }
}
