package com.adibarra.utils;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ADConfig {

    private final Map<String, String> config = new HashMap<>();
    private final Logger LOGGER;
    private final String PREFIX;
    private File configFile;

    /**
     * Get an ADConfig instance.
     *
     * @param name       the name of the mod
     * @param configPath the path to the config file
     */
    public ADConfig(String name, String configPath, String internalDefaultConfigPath) {
        LOGGER = LogManager.getLogger(name);
        PREFIX = "[" + name + "] [ADConfig] ";
        request(configPath, internalDefaultConfigPath);
    }

    /**
     * Load a config file. If the file doesn't exist, it will be generated.
     *
     * @param configPath                the path to the config file
     * @param internalDefaultConfigPath the path to the default config file (relative to jar base)
     */
    private void request(String configPath, String internalDefaultConfigPath) {
        String filename = configPath.substring(configPath.lastIndexOf('/') + 1);
        configFile = FabricLoader.getInstance().getConfigDir().resolve(filename).toFile();

        // create config file if it doesn't exist
        createConfig(configFile, internalDefaultConfigPath);

        // load config file
        List<String> configLines = loadFile(configFile);
        if (configLines.isEmpty()) {
            LOGGER.warn(PREFIX + "Config '{}' is blank! It is probably broken...", configFile.getName());
            deleteFile(configFile);
            return;
        }

        // parse config file
        parseConfig(configLines, configFile.getName());

        // migrate config if keys were added or removed
        migrateConfig(internalDefaultConfigPath);
    }

    /**
     * Checks if the user's config is missing keys (or has stale keys) compared to the
     * internal defaults. If so, regenerates the config file from defaults and reapplies
     * the user's existing values on top.
     *
     * @param internalDefaultConfigPath the path to the default config file (relative to jar base)
     */
    private void migrateConfig(String internalDefaultConfigPath) {
        List<String> defaultLines = loadInternalFile(internalDefaultConfigPath);
        if (defaultLines == null) return;

        // parse internal defaults to get expected keys
        Map<String, String> defaults = new HashMap<>();
        for (String line : defaultLines) {
            line = line.trim().toLowerCase();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] keyPair = line.split("=", 2);
            if (keyPair.length == 2) {
                defaults.put(keyPair[0].trim(), keyPair[1].trim());
            }
        }

        // check for missing or stale keys
        Set<String> missingKeys = new HashSet<>(defaults.keySet());
        missingKeys.removeAll(config.keySet());
        Set<String> staleKeys = new HashSet<>(config.keySet());
        staleKeys.removeAll(defaults.keySet());

        if (missingKeys.isEmpty() && staleKeys.isEmpty()) return;

        if (!missingKeys.isEmpty()) LOGGER.info(PREFIX + "Config migration: adding {} new option(s): {}", missingKeys.size(), missingKeys);
        if (!staleKeys.isEmpty()) LOGGER.info(PREFIX + "Config migration: removing {} stale option(s): {}", staleKeys.size(), staleKeys);

        // save user's current values
        Map<String, String> userValues = new HashMap<>(config);

        // regenerate config file from internal defaults
        deleteFile(configFile);
        createConfig(configFile, internalDefaultConfigPath);

        // reload fresh config
        config.clear();
        List<String> freshLines = loadFile(configFile);
        parseConfig(freshLines, configFile.getName());

        // reapply user values for keys that still exist
        for (Map.Entry<String, String> entry : userValues.entrySet()) {
            if (config.containsKey(entry.getKey())) {
                set(entry.getKey(), entry.getValue());
            }
        }

        LOGGER.info(PREFIX + "Config migration complete!");
    }

    /**
     * Attempts to create a new config file.
     *
     * @param configFile                the config file to create
     * @param internalDefaultConfigPath the path to the default config file (relative to jar base)
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createConfig(File configFile, String internalDefaultConfigPath) {
        if (!configFile.exists()) {
            LOGGER.warn(PREFIX + "Failed to find '{}'. Generating default...", configFile.getName());

            try {
                // try creating missing dirs and file
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();

            } catch (IOException e) {
                LOGGER.error(PREFIX + "Failed to create '{}'!", configFile.getName());
                LOGGER.trace(e);
                return;
            }

            // load default config from jar
            List<String> defaultConfigLines = loadInternalFile(internalDefaultConfigPath);
            if (defaultConfigLines == null) {
                LOGGER.error(PREFIX + "Failed to load default config file!");
                LOGGER.error(PREFIX + "Please report this to the mod author!");
                return;
            }

            // write default config to file
            if (writeFile(configFile, defaultConfigLines)) {
                LOGGER.info(PREFIX + "Generated new '{}'!", configFile.getName());
            }
        }
    }

    /**
     * Attempts to write a list of lines to a file.
     *
     * @param file  the file to write to
     * @param lines the lines to write
     * @return true if successful, false otherwise
     */
    private boolean writeFile(File file, List<String> lines) {
        try {
            PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8);
            writer.write(String.join("\n", lines));
            writer.close();
            return true;

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to write '{}'!", file.getName());
            LOGGER.trace(e);
            return false;
        }
    }

    /**
     * Attempts to load a file from the filesystem.
     *
     * @param file the file to load
     * @return the file's contents, or null if the file doesn't exist
     */
    private List<String> loadFile(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
            List<String> lines = br.lines().collect(Collectors.toList());
            br.close();
            return lines;

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to load '{}'!", file.getName());
            LOGGER.trace(e);
            return Collections.emptyList();
        }
    }

    /**
     * Attempts to load a file from the jar.
     *
     * @param path path to the file (relative to jar base)
     * @return the file's contents as a List, or null if the file doesn't exist
     */
    private List<String> loadInternalFile(String path) {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) return null;
        return new BufferedReader(new InputStreamReader(is)).lines().toList();
    }

    /**
     * Attempts to delete a file.
     *
     * @param file the file to delete
     */
    private void deleteFile(File file) {
        try {
            if (file.delete()) {
                LOGGER.info(PREFIX + "Deleted '{}'. Restart the game to regenerate it.", file.getName());
                return;
            }
            LOGGER.error(PREFIX + "Failed to delete '{}'! Is it in use?", file.getName());

        } catch (Exception e) {
            LOGGER.error(PREFIX + "Failed to delete '{}'!", file.getName());
            LOGGER.trace(e);
        }
    }

    /**
     * Attempts to replace a value in the config file.
     *
     * @param file  the config file
     * @param key   the key to replace
     * @param value the value to replace with
     */
    private void replaceValue(File file, String key, String value) {
        List<String> lines = loadFile(configFile);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().toLowerCase().startsWith(key + "=")) {
                String spaces = line.substring(0, line.indexOf(key));
                lines.set(i, spaces + key + "=" + value);
                writeFile(configFile, lines);
                return;
            }
        }
    }

    /**
     * Attempts to parse config from a List of lines.
     *
     * @param lines the List of config lines
     */
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
     * Sets a key's config value and updates the config file.
     *
     * @param key   the key to set
     * @param value the value to set
     * @return true if the key exists, false otherwise
     */
    public boolean set(String key, String value) {
        if (config.containsKey(key)) {
            config.put(key, value);
            replaceValue(configFile, key, value);
            return true;
        }
        return false;
    }

    /**
     * Overwrites config entries in-memory without writing to disk.
     *
     * @param data the entries to set
     */
    public void setAll(Map<String, String> data) {
        config.putAll(data);
    }

    /**
     * Gets a list of key value pairs in the config file.
     *
     * @return a list of key value pairs
     */
    public List<Map.Entry<String, String>> getEntries() {
        return new ArrayList<>(config.entrySet());
    }

    /**
     * Gets a list of all keys in the config file.
     *
     * @return a list of all keys
     */
    public List<String> getKeys() {
        return new ArrayList<>(config.keySet());
    }

    /**
     * Gets a list of all values in the config file.
     *
     * @return a list of all values
     */
    public List<String> getValues() {
        return new ArrayList<>(config.values());
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    public String getOrDefault(String key, String def) {
        String val = config.get(key);
        return val == null ? def : val;
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    public boolean getOrDefault(String key, boolean def) {
        String val = config.get(key);
        if (val == null) return def;
        return Boolean.parseBoolean(val);
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    public int getOrDefault(String key, int def) {
        String val = config.get(key);
        if (val == null) return def;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    public double getOrDefault(String key, double def) {
        String val = config.get(key);
        if (val == null) return def;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    public float getOrDefault(String key, float def) {
        String val = config.get(key);
        if (val == null) return def;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
