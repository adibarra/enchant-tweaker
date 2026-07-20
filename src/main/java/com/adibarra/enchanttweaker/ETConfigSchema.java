package com.adibarra.enchanttweaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** static schema describing the value type of every config key, inferred from the bundled default properties file */
public final class ETConfigSchema {

    public enum ValueType { BOOLEAN, INTEGER, DECIMAL, LIST }

    private static final Logger LOGGER = LoggerFactory.getLogger(EnchantTweaker.MOD_NAME);
    private static final String DEFAULT_CONFIG_PATH = "assets/enchanttweaker/enchant-tweaker.properties";

    // linkedHashMap/LinkedHashSet everywhere so category and key iteration order matches file order
    private static final Map<String, ValueType> SCHEMA = new LinkedHashMap<>();
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
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
            for (String raw : loadDefaults()) {
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) continue;

                // category header detection must run before the generic comment skip below,
                // because section headers also begin with '#'
                String slug = parseCategoryHeader(trimmed);
                if (slug != null) {
                    currentCategory = slug;
                    CATEGORY_ORDER.add(slug);
                    CATEGORY_KEYS.computeIfAbsent(slug, k -> new ArrayList<>());
                    continue;
                }

                if (trimmed.startsWith("#")) continue;

                String line = trimmed.toLowerCase();
                String[] keyPair = line.split("=", 2);
                if (keyPair.length != 2) continue;

                String key = keyPair[0].trim();
                String value = keyPair[1].trim();
                SCHEMA.put(key, inferType(value));
                DEFAULTS.put(key, value);
                if (RESERVED.contains(key)) {
                    CATEGORY_OF.put(key, "internal");
                } else if (currentCategory != null) {
                    CATEGORY_OF.put(key, currentCategory);
                    CATEGORY_KEYS.get(currentCategory).add(key);
                }
            }
        } catch (Exception e) {
            // keep loading if the bundled schema cannot be read
            SCHEMA.clear();
            DEFAULTS.clear();
            CATEGORY_OF.clear();
            CATEGORY_KEYS.clear();
            CATEGORY_ORDER.clear();
            LOGGER.error(EnchantTweaker.PREFIX + "Failed to load config schema! Validation will be permissive.", e);
        }
    }

    /** detects a top-level category header */
    private static String parseCategoryHeader(String trimmed) {
        if (!trimmed.startsWith("###") || trimmed.startsWith("####")) return null;
        String rest = trimmed.substring(3).trim();
        if (rest.isEmpty() || rest.startsWith("#") || !rest.endsWith(":")) return null;
        return rest.substring(0, rest.length() - 1).trim().toLowerCase().replace(' ', '_');
    }

    /** loads the bundled default config lines from the classpath */
    private static List<String> loadDefaults() {
        InputStream is = ETConfigSchema.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH);
        if (is == null) return Collections.emptyList();
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().toList();
    }

    /** infers the config value type */
    private static ValueType inferType(String value) {
        if (value.equals("true") || value.equals("false")) return ValueType.BOOLEAN;
        try {
            Integer.parseInt(value);
            return ValueType.INTEGER;
        } catch (NumberFormatException ignored) { }
        try {
            Double.parseDouble(value);
            return ValueType.DECIMAL;
        } catch (NumberFormatException ignored) { }
        return ValueType.LIST;
    }

    /** gets the inferred type of a config key */
    public static ValueType typeOf(String key) {
        return SCHEMA.get(key);
    }

    /** reports whether a key is reserved */
    public static boolean isReserved(String key) {
        return RESERVED.contains(key);
    }

    /** gets the bundled default value for a key */
    public static String defaultOf(String key) {
        return DEFAULTS.get(key);
    }

    /** gets every non-reserved key's bundled default value, in file order */
    public static Map<String, String> defaults() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (!RESERVED.contains(entry.getKey())) out.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(out);
    }

    /** gets the category slug a key belongs to */
    public static String categoryOf(String key) {
        return CATEGORY_OF.get(key);
    }

    /** gets the category slugs in file order, excluding categories that contain no non-reserved keys */
    public static List<String> categories() {
        List<String> out = new ArrayList<>();
        for (String slug : CATEGORY_ORDER) {
            List<String> keys = CATEGORY_KEYS.get(slug);
            if (keys != null && !keys.isEmpty()) out.add(slug);
        }
        return out;
    }

    /** gets the non-reserved keys in a category, in file order */
    public static List<String> keysIn(String categorySlug) {
        List<String> keys = CATEGORY_KEYS.get(categorySlug);
        return keys == null ? Collections.emptyList() : new ArrayList<>(keys);
    }

    /** validates a value against its key's inferred type */
    public static boolean isValid(String key, String value) {
        ValueType type = SCHEMA.get(key);
        if (type == null) return true;
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
        ValueType type = SCHEMA.get(key);
        if (type == null) return "";
        return switch (type) {
            case BOOLEAN -> "true/false";
            case INTEGER -> "an integer";
            case DECIMAL -> "a number";
            case LIST -> "a comma-separated list";
        };
    }
}
