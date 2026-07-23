package com.adibarra.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class ADConfig {

    // config is read and written from multiple game threads
    private final Map<String, String> config = new ConcurrentHashMap<>();
    private final List<Migration> migrations;
    private final Logger LOGGER;
    private final String PREFIX;
    private File configFile;
    private boolean loaded;
    private boolean migrationWriteFailed;

    /**
     * config schema version from bundled defaults and migrations
     */
    public static final String VERSION_KEY = "config_version";

    // static logger for pure-static applyMigrations without instance context
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(ADConfig.class);

    /** a single config schema migration */
    public record Migration(int toVersion, java.util.function.Consumer<Map<String, String>> transform) {
    }

    /**
     * add migrations in ascending toVersion order apply when storedVersion &lt;
     * toVersion &lt;= currentVersion
     */
    private static final List<Migration> MIGRATIONS = List.of();

    /**
     * get an ADConfig instance
     *
     * @param name
     *            the name of the mod
     * @param configPath
     *            the path to the config file
     */
    public ADConfig(String name, String configPath, String internalDefaultConfigPath) {
        this(name, configPath, internalDefaultConfigPath, MIGRATIONS);
    }

    ADConfig(String name, String configPath, String internalDefaultConfigPath, List<Migration> migrations) {
        LOGGER = LoggerFactory.getLogger(name);
        PREFIX = "[" + name + "] [ADConfig] ";
        this.migrations = List.copyOf(migrations);
        request(configPath, internalDefaultConfigPath);
    }

    /**
     * loads or generates a config file
     *
     * @param configPath
     *            the path to the config file
     * @param internalDefaultConfigPath
     *            the path to the default config file (relative to jar base)
     */
    private void request(String configPath, String internalDefaultConfigPath) {
        String filename = configPath.substring(configPath.lastIndexOf('/') + 1);
        configFile = FabricLoader.getInstance().getConfigDir().resolve(filename).toFile();

        createConfig(configFile, internalDefaultConfigPath);

        List<String> configLines = loadFile(configFile);
        if (configLines == null) {
            LOGGER.warn(PREFIX + "Config '{}' could not be read; leaving the existing file untouched.",
                configFile.getName());
            return;
        }
        if (configLines.isEmpty()) {
            LOGGER.warn(PREFIX + "Config '{}' is blank! Regenerating from defaults...", configFile.getName());
        }

        parseConfig(configLines, configFile.getName());
        Map<String, String> originalConfig = new HashMap<>(config);
        int currentVersion = readBundledVersion(internalDefaultConfigPath);
        int storedVersion = parseVersion(config.get(VERSION_KEY));
        boolean newerSchema = currentVersion > 0 && storedVersion > currentVersion;
        if (newerSchema) {
            LOGGER.warn(PREFIX + "Stored config version {} is newer than supported version {}; preserving it.",
                storedVersion, currentVersion);
        }

        // run version migrations before key-diff migration
        // preserve raw keys before stale-key removal
        Map<String, String> beforeVersionMigration = currentVersion > 0 && storedVersion < currentVersion
            ? new HashMap<>(config)
            : null;
        boolean versionChanged = applyVersionMigrations(internalDefaultConfigPath);
        boolean versionValuesChanged = versionChanged && beforeVersionMigration != null
            && hasValueChanges(beforeVersionMigration, config);

        // migrate config when keys change or values were transformed
        // persist the in-memory version stamp with rewrites
        boolean migrationRewrote = !newerSchema && migrateConfig(internalDefaultConfigPath, versionValuesChanged);

        // persist version-only migrations
        // restore disk values if migration writes fail
        if (versionChanged && !migrationRewrote && !migrationWriteFailed
            && !set(VERSION_KEY, config.get(VERSION_KEY))) {
            migrationWriteFailed = true;
        }
        if (migrationWriteFailed) {
            config.clear();
            config.putAll(originalConfig);
        }
        loaded = true;
    }

    /** applies config migrations up to the bundled schema version */
    private boolean applyVersionMigrations(String internalDefaultConfigPath) {
        int currentVersion = readBundledVersion(internalDefaultConfigPath);
        if (currentVersion <= 0)
            return false; // machinery disabled: no config_version in the bundled defaults

        int storedVersion = parseVersion(config.get(VERSION_KEY));
        if (storedVersion >= currentVersion)
            return false;

        applyMigrations(config, migrations, storedVersion, currentVersion);
        return true;
    }

    /** applies version migrations to a config map */
    public static void applyMigrations(Map<String, String> config, List<Migration> migrations, int storedVersion,
        int currentVersion) {
        if (storedVersion > currentVersion) {
            STATIC_LOGGER.warn(
                "[ADConfig] Stored config version {} is newer than the supported version {}; keeping existing values.",
                storedVersion, currentVersion);
            config.putIfAbsent(VERSION_KEY, String.valueOf(storedVersion));
            return;
        }
        if (storedVersion < currentVersion) {
            migrations.stream().filter(m -> storedVersion < m.toVersion() && m.toVersion() <= currentVersion)
                .sorted(Comparator.comparingInt(Migration::toVersion)).forEach(m -> m.transform().accept(config));
        }
        config.put(VERSION_KEY, String.valueOf(currentVersion));
    }

    /** returns whether migrations changed a non-version value */
    private static boolean hasValueChanges(Map<String, String> before, Map<String, String> after) {
        for (Map.Entry<String, String> entry : before.entrySet()) {
            if (VERSION_KEY.equals(entry.getKey()))
                continue;
            if (!after.containsKey(entry.getKey()) || !Objects.equals(entry.getValue(), after.get(entry.getKey())))
                return true;
        }
        for (String key : after.keySet()) {
            if (!VERSION_KEY.equals(key) && !before.containsKey(key))
                return true;
        }
        return false;
    }

    /** reads #VERSION_KEY from the bundled defaults */
    private int readBundledVersion(String internalDefaultConfigPath) {
        List<String> defaultLines = loadInternalFile(internalDefaultConfigPath);
        if (defaultLines == null)
            return 0;
        for (String line : defaultLines) {
            String trimmed = line.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            String[] keyPair = trimmed.split("=", 2);
            if (keyPair.length == 2 && VERSION_KEY.equals(normalizeKey(keyPair[0])))
                return parseVersion(keyPair[1].trim());
        }
        return 0;
    }

    /** parses a version string, defaulting to 0 when null or non-numeric */
    private static int parseVersion(String value) {
        if (value == null)
            return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** normalizes a config key or returns null for malformed keys */
    private static String normalizeKey(String key) {
        if (key == null)
            return null;
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || normalized.charAt(0) == '#' || normalized.indexOf('=') >= 0
            || containsLineBreak(normalized) ? null : normalized;
    }

    /** returns the stored value for a normalized public key lookup */
    private String lookup(String key) {
        String normalized = normalizeKey(key);
        return normalized == null ? null : config.get(normalized);
    }

    /**
     * checks user keys against internal defaults removes stale keys and adds
     * missing keys
     */
    private boolean migrateConfig(String internalDefaultConfigPath, boolean forceRewrite) {
        List<String> defaultLines = loadInternalFile(internalDefaultConfigPath);
        if (defaultLines == null)
            return false;

        Map<String, String> defaults = new HashMap<>();
        for (String line : defaultLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            String[] keyPair = trimmed.split("=", 2);
            if (keyPair.length == 2) {
                String key = normalizeKey(keyPair[0]);
                if (key != null)
                    defaults.put(key, keyPair[1].trim());
            }
        }

        Set<String> missingKeys = new HashSet<>(defaults.keySet());
        missingKeys.removeAll(config.keySet());
        Set<String> staleKeys = new HashSet<>(config.keySet());
        staleKeys.removeAll(defaults.keySet());

        if (missingKeys.isEmpty() && staleKeys.isEmpty() && !forceRewrite)
            return false;

        if (!missingKeys.isEmpty())
            LOGGER.info(PREFIX + "Config migration: adding {} new option(s): {}", missingKeys.size(), missingKeys);
        if (!staleKeys.isEmpty())
            LOGGER.info(PREFIX + "Config migration: removing {} stale option(s): {}", staleKeys.size(), staleKeys);

        Map<String, String> userValues = new HashMap<>(config);

        // rebuild defaults with user values and preserve formatting
        // write the file exactly once
        List<String> newLines = new ArrayList<>(defaultLines.size());
        for (String rawLine : defaultLines) {
            String trimmed = rawLine.trim();
            int eq = rawLine.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 0) {
                newLines.add(rawLine);
                continue;
            }
            String key = normalizeKey(rawLine.substring(0, eq));
            if (key == null) {
                newLines.add(rawLine);
                continue;
            }
            String value = userValues.getOrDefault(key, defaults.get(key));
            newLines.add(rawLine.substring(0, eq + 1) + value);
        }

        Map<String, String> migratedValues = new HashMap<>();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            migratedValues.put(entry.getKey(), userValues.getOrDefault(entry.getKey(), entry.getValue()));
        }

        if (!writeFile(configFile, newLines)) {
            migrationWriteFailed = true;
            LOGGER.error(PREFIX + "Config migration could not be written; keeping the existing config file.");
            return false;
        }

        config.clear();
        config.putAll(migratedValues);
        LOGGER.info(PREFIX + "Config migration complete!");
        return true;
    }

    /**
     * attempts to create a new config file
     *
     * @param configFile
     *            the config file to create
     * @param internalDefaultConfigPath
     *            the path to the default config file (relative to jar base)
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createConfig(File configFile, String internalDefaultConfigPath) {
        if (!configFile.exists()) {
            LOGGER.warn(PREFIX + "Failed to find '{}'. Generating default...", configFile.getName());

            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();

            } catch (IOException e) {
                LOGGER.error(PREFIX + "Failed to create '{}'!", configFile.getName(), e);
                return;
            }

            List<String> defaultConfigLines = loadInternalFile(internalDefaultConfigPath);
            if (defaultConfigLines == null) {
                LOGGER.error(PREFIX + "Failed to load default config file!");
                LOGGER.error(PREFIX + "Please report this to the mod author!");
                return;
            }

            if (writeFile(configFile, defaultConfigLines)) {
                LOGGER.info(PREFIX + "Generated new '{}'!", configFile.getName());
            }
        }
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    /**
     * attempts to write a list of lines to a file
     *
     * @param file
     *            the file to write to
     * @param lines
     *            the lines to write
     * @return true if successful, false otherwise
     */
    private boolean writeFile(File file, List<String> lines) {
        if (file == null || lines == null || lines.stream().anyMatch(line -> line == null || containsLineBreak(line)))
            return false;
        Path target = file.toPath().toAbsolutePath();
        Path temporary = null;
        try {
            String prefix = file.getName() + ".";
            if (prefix.length() < 3)
                prefix = "cfg-" + prefix;
            temporary = Files.createTempFile(target.getParent(), prefix, ".tmp");
            Files.writeString(temporary, String.join("\n", lines), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to write '{}'!", file.getName(), e);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    LOGGER.warn(PREFIX + "Failed to remove temporary config file '{}'.", temporary, e);
                }
            }
        }
    }

    /**
     * attempts to load a file from the filesystem
     *
     * @param file
     *            the file to load
     * @return the file's contents, or null if it cannot be read
     */
    private List<String> loadFile(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to load '{}'!", file.getName(), e);
            return null;
        }
    }

    /**
     * attempts to load a file from the jar
     *
     * @param path
     *            path to the file (relative to jar base)
     * @return the file's contents as a List, or null if the file doesn't exist
     */
    private List<String> loadInternalFile(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                return null;
            // read with an explicit UTF-8 decoder and close the stream via
            // try-with-resources
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().toList();

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to load internal file '{}'!", path, e);
            return null;
        }
    }

    /**
     * attempts to replace a value in the config file
     *
     * @param file
     *            the config file
     * @param key
     *            the key to replace
     * @param value
     *            the value to replace with
     */
    private boolean replaceValue(File file, String key, String value) {
        if (normalizeKey(key) == null)
            return false;
        if (value == null || containsLineBreak(value))
            return false;
        List<String> loadedLines = loadFile(file);
        if (loadedLines == null)
            return false;
        List<String> lines = new ArrayList<>(loadedLines);
        boolean matched = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int equals = line.indexOf('=');
            if (equals < 0 || !line.substring(0, equals).trim().equalsIgnoreCase(key))
                continue;
            int valueStart = equals + 1;
            while (valueStart < line.length() && Character.isWhitespace(line.charAt(valueStart)))
                valueStart++;
            lines.set(i, line.substring(0, valueStart) + value);
            matched = true;
        }
        if (!matched)
            lines.add(key + "=" + value);
        return writeFile(file, lines);
    }

    /** attempts to parse config from a List of lines */
    private void parseConfig(List<String> lines, String filename) {
        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;

            String[] keyPair = trimmed.split("=", 2);
            if (keyPair.length != 2) {
                LOGGER.warn(PREFIX + "'{}' line {}: Found a syntax error! Skipping line...", filename, lineNum);
                continue;
            }
            String key = normalizeKey(keyPair[0]);
            if (key == null) {
                LOGGER.warn(PREFIX + "'{}' line {}: Found a syntax error! Skipping line...", filename, lineNum);
                continue;
            }
            config.put(key, keyPair[1].trim());
        }
    }
    /**
     * sets a key's config value and updates the config file
     *
     * @param key
     *            the key to set
     * @param value
     *            the value to set
     * @return true if the key exists, false otherwise
     */
    public synchronized boolean set(String key, String value) {
        // guard nulls: ConcurrentHashMap.containsKey/put both throw NPE on a null
        // key/value
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null || value == null || containsLineBreak(value))
            return false;
        if (config.containsKey(normalizedKey) && replaceValue(configFile, normalizedKey, value)) {
            config.put(normalizedKey, value);
            return true;
        }
        return false;
    }

    /**
     * overwrites config entries in-memory without writing to disk
     *
     * @param data
     *            the entries to set
     */
    public void setAll(Map<String, String> data) {
        if (data == null)
            return;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (key != null && entry.getValue() != null && !containsLineBreak(entry.getValue()))
                config.put(key, entry.getValue());
        }
    }

    /** gets the absolute path of the loaded config file */
    public String getConfigPath() {
        return configFile == null ? null : configFile.getAbsolutePath();
    }

    /** returns whether the configuration file was successfully read */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * overwrites config entries in-memory and persists them to disk in a single
     * batched write
     *
     * @return true when all sanitized updates were persisted
     */
    public synchronized boolean setAllAndPersist(Map<String, String> data) {
        if (data == null)
            return true;
        // sanitize: drop malformed keys/null values and lowercase valid keys
        Map<String, String> updates = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (key != null && entry.getValue() != null && !containsLineBreak(entry.getValue()))
                updates.put(key, entry.getValue());
        }
        if (updates.isEmpty())
            return true;

        List<String> loadedLines = loadFile(configFile);
        if (loadedLines == null)
            return false;
        List<String> lines = new ArrayList<>(loadedLines);
        Set<String> unwritten = new HashSet<>(updates.keySet());
        for (int i = 0; i < lines.size(); i++) {
            String rawLine = lines.get(i);
            String trimmed = rawLine.trim();
            int eq = rawLine.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 0)
                continue;
            String key = normalizeKey(rawLine.substring(0, eq));
            if (key == null)
                continue;
            if (updates.containsKey(key)) {
                lines.set(i, rawLine.substring(0, eq + 1) + updates.get(key));
                unwritten.remove(key);
            }
        }
        // no matching line found for these keys (e.g. hand-removed): append them
        for (String key : unwritten) {
            lines.add(key + "=" + updates.get(key));
        }
        if (!writeFile(configFile, lines))
            return false;
        config.putAll(updates);
        return true;
    }

    /**
     * gets a list of key value pairs in the config file
     *
     * @return a list of key value pairs
     */
    public List<Map.Entry<String, String>> getEntries() {
        return new ArrayList<>(config.entrySet());
    }

    /**
     * gets a list of all keys in the config file
     *
     * @return a list of all keys
     */
    public List<String> getKeys() {
        return new ArrayList<>(config.keySet());
    }

    /**
     * attempt to get a key value from the config file
     *
     * @return the key value, or def if the key is missing
     */
    public String getOrDefault(String key, String def) {
        String val = lookup(key);
        return val == null ? def : val;
    }

    /**
     * attempt to get a key value from the config file
     *
     * @return the key value, or def if the key is missing
     */
    public boolean getOrDefault(String key, boolean def) {
        String val = lookup(key);
        if (val == null)
            return def;
        if (val.equalsIgnoreCase("true"))
            return true;
        if (val.equalsIgnoreCase("false"))
            return false;
        return def;
    }

    /**
     * attempt to get a key value from the config file
     *
     * @return the key value, or def if the key is missing
     */
    public int getOrDefault(String key, int def) {
        String val = lookup(key);
        if (val == null)
            return def;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * attempt to get a key value from the config file
     *
     * @return the key value, or def if the key is missing
     */
    public double getOrDefault(String key, double def) {
        String val = lookup(key);
        if (val == null)
            return def;
        try {
            double parsed = Double.parseDouble(val);
            return Double.isFinite(parsed) ? parsed : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * attempt to get a key value from the config file
     *
     * @return the key value, or def if the key is missing
     */
    public float getOrDefault(String key, float def) {
        String val = lookup(key);
        if (val == null)
            return def;
        try {
            float parsed = Float.parseFloat(val);
            return Float.isFinite(parsed) ? parsed : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
