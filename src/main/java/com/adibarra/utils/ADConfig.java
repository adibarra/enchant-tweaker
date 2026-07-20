package com.adibarra.utils;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class ADConfig {

    // config is read and written from multiple game threads
    private final Map<String, String> config = new ConcurrentHashMap<>();
    private final Logger LOGGER;
    private final String PREFIX;
    private File configFile;

    /** config key holding the schema version. Read from the bundled defaults; drives {@link #applyMigrations} */
    public static final String VERSION_KEY = "config_version";

    // static logger for the pure-static applyMigrations helper, which has no instance context
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(ADConfig.class);

    /** a single config schema migration */
    public record Migration(int toVersion, java.util.function.Consumer<Map<String, String>> transform) { }

    /** add entries in ascending toVersion order; applied when storedVersion &lt; toVersion &lt;= currentVersion */
    private static final List<Migration> MIGRATIONS = List.of();

    /**
     * get an ADConfig instance
     *
     * @param name the name of the mod
     * @param configPath the path to the config file
     */
    public ADConfig(String name, String configPath, String internalDefaultConfigPath) {
        LOGGER = LoggerFactory.getLogger(name);
        PREFIX = "[" + name + "] [ADConfig] ";
        request(configPath, internalDefaultConfigPath);
    }

    /**
     * load a config file. If the file doesn't exist, it will be generated
     *
     * @param configPath the path to the config file
     * @param internalDefaultConfigPath the path to the default config file (relative to jar base)
     */
    private void request(String configPath, String internalDefaultConfigPath) {
        String filename = configPath.substring(configPath.lastIndexOf('/') + 1);
        configFile = FabricLoader.getInstance().getConfigDir().resolve(filename).toFile();

        createConfig(configFile, internalDefaultConfigPath);

        List<String> configLines = loadFile(configFile);
        if (configLines.isEmpty()) {
            // a blank or unreadable file must not disable the mod: fall through to migrate,
            // which regenerates the file from the bundled defaults
            LOGGER.warn(PREFIX + "Config '{}' is blank or unreadable! Regenerating from defaults...", configFile.getName());
        }

        parseConfig(configLines, configFile.getName());

        // run version-based migrations BEFORE the key-diff migrateConfig, so a future rename
        // transform can see the user's raw pre-diff key before key-diff drops it as stale
        boolean versionChanged = applyVersionMigrations(internalDefaultConfigPath);

        // migrate config if keys were added or removed. When it rewrites the file, the in-memory
        // version_KEY stamp set above is carried to disk in that same batched write
        boolean keyDiffRewrote = migrateConfig(internalDefaultConfigPath);

        // version bumped but no key-diff rewrite persisted it: stamp the bump in one write
        if (versionChanged && !keyDiffRewrote) {
            set(VERSION_KEY, config.get(VERSION_KEY));
        }
    }

    /** applies config migrations up to the bundled schema version */
    private boolean applyVersionMigrations(String internalDefaultConfigPath) {
        int currentVersion = readBundledVersion(internalDefaultConfigPath);
        if (currentVersion <= 0) return false; // machinery disabled: no config_version in the bundled defaults

        int storedVersion = parseVersion(config.get(VERSION_KEY));
        if (storedVersion == currentVersion) return false;

        applyMigrations(config, MIGRATIONS, storedVersion, currentVersion);
        return true;
    }

    /** applies version migrations to a config map */
    public static void applyMigrations(Map<String, String> config, List<Migration> migrations, int storedVersion, int currentVersion) {
        if (storedVersion > currentVersion) {
            STATIC_LOGGER.warn("[ADConfig] Stored config version {} is newer than the supported version {}; keeping existing values.", storedVersion, currentVersion);
        } else if (storedVersion < currentVersion) {
            migrations.stream()
                .filter(m -> storedVersion < m.toVersion() && m.toVersion() <= currentVersion)
                .sorted(Comparator.comparingInt(Migration::toVersion))
                .forEach(m -> m.transform().accept(config));
        }
        config.put(VERSION_KEY, String.valueOf(currentVersion));
    }

    /** reads #VERSION_KEY from the bundled defaults */
    private int readBundledVersion(String internalDefaultConfigPath) {
        List<String> defaultLines = loadInternalFile(internalDefaultConfigPath);
        if (defaultLines == null) return 0;
        for (String line : defaultLines) {
            String trimmed = line.trim().toLowerCase();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] keyPair = trimmed.split("=", 2);
            if (keyPair.length == 2 && keyPair[0].trim().equals(VERSION_KEY)) {
                return parseVersion(keyPair[1].trim());
            }
        }
        return 0;
    }

    /** parses a version string, defaulting to 0 when null or non-numeric */
    private static int parseVersion(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** checks if the user's config is missing keys (or has stale keys) compared to the internal defaults */
    private boolean migrateConfig(String internalDefaultConfigPath) {
        List<String> defaultLines = loadInternalFile(internalDefaultConfigPath);
        if (defaultLines == null) return false;

        Map<String, String> defaults = new HashMap<>();
        for (String line : defaultLines) {
            line = line.trim().toLowerCase();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] keyPair = line.split("=", 2);
            if (keyPair.length == 2) {
                defaults.put(keyPair[0].trim(), keyPair[1].trim());
            }
        }

        Set<String> missingKeys = new HashSet<>(defaults.keySet());
        missingKeys.removeAll(config.keySet());
        Set<String> staleKeys = new HashSet<>(config.keySet());
        staleKeys.removeAll(defaults.keySet());

        if (missingKeys.isEmpty() && staleKeys.isEmpty()) return false;

        if (!missingKeys.isEmpty()) LOGGER.info(PREFIX + "Config migration: adding {} new option(s): {}", missingKeys.size(), missingKeys);
        if (!staleKeys.isEmpty()) LOGGER.info(PREFIX + "Config migration: removing {} stale option(s): {}", staleKeys.size(), staleKeys);

        Map<String, String> userValues = new HashMap<>(config);

        // rebuild the file content in memory from the bundled defaults, substituting the user's
        // existing values line-by-line (preserving the defaults' comments and formatting), and
        // write the file exactly once
        List<String> newLines = new ArrayList<>(defaultLines.size());
        for (String rawLine : defaultLines) {
            String trimmed = rawLine.trim();
            int eq = rawLine.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 0) {
                newLines.add(rawLine);
                continue;
            }
            String key = rawLine.substring(0, eq).trim().toLowerCase();
            String value = userValues.getOrDefault(key, defaults.get(key));
            newLines.add(rawLine.substring(0, eq + 1) + value);
        }

        config.clear();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            config.put(entry.getKey(), userValues.getOrDefault(entry.getKey(), entry.getValue()));
        }

        writeFile(configFile, newLines);

        LOGGER.info(PREFIX + "Config migration complete!");
        return true;
    }

    /**
     * attempts to create a new config file
     *
     * @param configFile the config file to create
     * @param internalDefaultConfigPath the path to the default config file (relative to jar base)
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

    /**
     * attempts to write a list of lines to a file
     *
     * @param file the file to write to
     * @param lines the lines to write
     * @return true if successful, false otherwise
     */
    private boolean writeFile(File file, List<String> lines) {
        try {
            // files.write surfaces write failures as IOException (PrintWriter silently swallowed them)
            Files.write(file.toPath(), String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            return true;

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to write '{}'!", file.getName(), e);
            return false;
        }
    }

    /**
     * attempts to load a file from the filesystem
     *
     * @param file the file to load
     * @return the file's contents, or null if the file doesn't exist
     */
    private List<String> loadFile(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to load '{}'!", file.getName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * attempts to load a file from the jar
     *
     * @param path path to the file (relative to jar base)
     * @return the file's contents as a List, or null if the file doesn't exist
     */
    private List<String> loadInternalFile(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            // read with an explicit UTF-8 decoder and close the stream via try-with-resources
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().toList();

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to load internal file '{}'!", path, e);
            return null;
        }
    }

    /**
     * attempts to replace a value in the config file
     *
     * @param file the config file
     * @param key the key to replace
     * @param value the value to replace with
     */
    private void replaceValue(File file, String key, String value) {
        // wrap in a mutable list: loadFile can return an immutable empty list on I/O failure
        List<String> lines = new ArrayList<>(loadFile(file));
        String lowerKey = key.toLowerCase();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().toLowerCase().startsWith(lowerKey + "=")) {
                // match is case-insensitive, so locate the offset case-insensitively too:
                // a case-sensitive indexOf(key) throws on a hand-edited mixed-case key
                String spaces = line.substring(0, line.toLowerCase().indexOf(lowerKey));
                lines.set(i, spaces + key + "=" + value);
                writeFile(file, lines);
                return;
            }
        }
        // no matching line found (e.g. a hand-removed key): append it instead of doing nothing
        lines.add(key + "=" + value);
        writeFile(file, lines);
    }

    /** attempts to parse config from a List of lines */
    private void parseConfig(List<String> lines, String filename) {
        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            line = line.trim().toLowerCase();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] keyPair = line.split("=", 2);
            if (keyPair.length != 2) {
                LOGGER.warn(PREFIX + "'{}' line {}: Found a syntax error! Skipping line...", filename, lineNum);
                continue;
            }

            config.put(keyPair[0].trim(), keyPair[1].trim());
        }
    }

    /**
     * sets a key's config value and updates the config file
     *
     * @param key the key to set
     * @param value the value to set
     * @return true if the key exists, false otherwise
     */
    public boolean set(String key, String value) {
        // guard nulls: ConcurrentHashMap.containsKey/put both throw NPE on a null key/value
        if (key == null || value == null) return false;
        if (config.containsKey(key)) {
            config.put(key, value);
            replaceValue(configFile, key, value);
            return true;
        }
        return false;
    }

    /**
     * overwrites config entries in-memory without writing to disk
     *
     * @param data the entries to set
     */
    public void setAll(Map<String, String> data) {
        config.putAll(data);
    }

    /** gets the absolute path of the loaded config file */
    public String getConfigPath() {
        return configFile == null ? null : configFile.getAbsolutePath();
    }

    /** overwrites config entries in-memory and persists them to disk in a single batched write */
    public void setAllAndPersist(Map<String, String> data) {
        if (data == null) return;

        // sanitize: drop null keys/values (ConcurrentHashMap rejects them) and lowercase keys to
        // match the key convention used everywhere else in ADConfig
        Map<String, String> updates = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                updates.put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }
        if (updates.isEmpty()) return;

        config.putAll(updates);

        // one batched file rewrite: substitute values for all matching keys in the existing lines
        List<String> lines = new ArrayList<>(loadFile(configFile));
        Set<String> unwritten = new HashSet<>(updates.keySet());
        for (int i = 0; i < lines.size(); i++) {
            String rawLine = lines.get(i);
            String trimmed = rawLine.trim();
            int eq = rawLine.indexOf('=');
            if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 0) continue;
            String key = rawLine.substring(0, eq).trim().toLowerCase();
            if (updates.containsKey(key)) {
                lines.set(i, rawLine.substring(0, eq + 1) + updates.get(key));
                unwritten.remove(key);
            }
        }
        // no matching line found for these keys (e.g. hand-removed): append them
        for (String key : unwritten) {
            lines.add(key + "=" + updates.get(key));
        }
        writeFile(configFile, lines);
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
        if (key == null) return def;
        String val = config.get(key);
        return val == null ? def : val;
    }

    /**
     * attempt to get a key value from the config file
     *
     * @return the key value, or def if the key is missing
     */
    public boolean getOrDefault(String key, boolean def) {
        if (key == null) return def;
        String val = config.get(key);
        if (val == null) return def;
        if (val.equalsIgnoreCase("true")) return true;
        if (val.equalsIgnoreCase("false")) return false;
        return def;
    }

    /**
     * attempt to get a key value from the config file
     *
     * @return the key value, or def if the key is missing
     */
    public int getOrDefault(String key, int def) {
        if (key == null) return def;
        String val = config.get(key);
        if (val == null) return def;
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
        if (key == null) return def;
        String val = config.get(key);
        if (val == null) return def;
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
        if (key == null) return def;
        String val = config.get(key);
        if (val == null) return def;
        try {
            float parsed = Float.parseFloat(val);
            return Float.isFinite(parsed) ? parsed : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
