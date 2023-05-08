package com.adibarra.utils;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unused")
public class ADConfig {

    private final HashMap<String, String> config = new HashMap<>();
    private final Logger LOGGER;
    private final String PREFIX;

    /**
     * Loads a config file from the config directory. If the file doesn't exist, it will be generated.
     *
     * @param name          the name of the mod
     * @param defaultConfig the path to the default config file (relative to jar base)
     */
    public ADConfig(String name, String defaultConfig) {
        LOGGER = LogManager.getLogger(name);
        PREFIX = "[" + name + "] [ADConfig] ";
        this.request(defaultConfig);
    }

    /**
     * Attempts to load a config file. If the file doesn't exist, it will be generated.
     *
     * @param defaultConfig the contents of the default config file
     */
    private void request(String defaultConfig) {
        String filename = defaultConfig.substring(defaultConfig.lastIndexOf('/') + 1);
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = configDir.resolve(filename).toFile();

        // create config file if it doesn't exist
        createConfig(configFile, defaultConfig);

        // load config file
        List<String> configLines = loadFile(configFile);
        if (configLines.isEmpty()) {
            LOGGER.warn(PREFIX + "Config '{}' is blank! It is probably broken...", configFile.getName());
            deleteFile(configFile);

        } else {
            // parse config file
            parseConfig(configLines, configFile.getName());
        }
    }

    /**
     * Attempts to create a new config file.
     *
     * @param configFile    the config file to create
     * @param defaultConfig the contents of the default config file
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createConfig(File configFile, String defaultConfig) {
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

            // write default config to file
            List<String> defaultConfigLines = loadInternalFile(defaultConfig);
            if (defaultConfigLines == null) {
                LOGGER.error(PREFIX + "Failed to load default config file!");
                LOGGER.error(PREFIX + "Please report this to the mod author!");

            } else if (writeFile(configFile, defaultConfigLines)) {
                LOGGER.info(PREFIX + "Generated new '{}'!", configFile.getName());
            }
        }
    }

    /**
     * Attempts to write a list of lines to a file.
     *
     * @param lines the lines to write
     * @return true if the file was written successfully, false otherwise
     */
    private boolean writeFile(File file, List<String> lines) {
        try {
            PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8);
            writer.write(String.join("\n", lines));
            writer.close();
            return true;

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to generate '{}'!", file.getName());
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
            List<String> lines = br.lines().toList();
            br.close();
            return lines;

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to load '{}'!", file.getName());
            LOGGER.trace(e);
            return Collections.emptyList();
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

            String[] keyPair = line.split("=");
            if (keyPair.length != 2) {
                LOGGER.warn(PREFIX + "'{}' line {}: Found a syntax error! Skipping line...", filename, lineNum);
                continue;
            }

            config.put(keyPair[0].trim(), keyPair[1].trim());
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

    // TODO: Set should also save to config file, not just in-memory.
    // TODO: Should be done via searching for the key and only updating the value.

    /**
     * Sets a key's config value.
     *
     * @param key   the key to set
     * @param value the value to set
     * @return true if the key exists, false otherwise
     */
    public boolean set(String key, String value) {
        if (config.containsKey(key)) {
            config.put(key, value);
            return true;
        }
        return false;
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
    @SuppressWarnings("unused")
    public String getOrDefault(String key, String def) {
        String val = config.get(key);
        return val == null ? def : val;
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    @SuppressWarnings("unused")
    public boolean getOrDefault(String key, boolean def) {
        try {
            return Boolean.parseBoolean(config.get(key));
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    @SuppressWarnings("unused")
    public int getOrDefault(String key, int def) {
        try {
            return Integer.parseInt(config.get(key));
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    @SuppressWarnings("unused")
    public double getOrDefault(String key, double def) {
        try {
            return Double.parseDouble(config.get(key));
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Attempt to get a key value from the config file.
     *
     * @return the key value, or def if the key is missing.
     */
    @SuppressWarnings("unused")
    public float getOrDefault(String key, float def) {
        try {
            return Float.parseFloat(config.get(key));
        } catch (Exception e) {
            return def;
        }
    }
}
