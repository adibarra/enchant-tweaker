package com.adibarra.utils;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ADConfig {
    private final HashMap<String, String> config = new HashMap<>();
    private final Logger LOGGER;
    private final String PREFIX;

    /**
     * Loads a config file from the config directory. If the file doesn't exist, it will be generated.
     * @param name the name of the mod
     * @param defaultConfig the path to the default config file (relative to jar base)
     */
    public ADConfig(String name, String defaultConfig) {
        LOGGER = LogManager.getLogger(name);
        PREFIX = "[" + name + "] [ADConfig] ";
        this.request(defaultConfig);
    }

    /**
     * Attempts to load a config file. If the file doesn't exist, it will be generated.
     * @param defaultConfig the contents of the default config file
     */
    private void request(String defaultConfig) {
        String filename = defaultConfig.substring(defaultConfig.lastIndexOf('/') + 1);
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = configDir.resolve(filename).toFile();

        createConfig(configFile, defaultConfig);
        loadConfig(configFile);
    }

    /**
     * Loads a default config file within the jar.
     * @param defaultConfig the path to the default config file (relative to jar base)
     * @return the config file's contents, or null if the file doesn't exist
     */
    private String getDefaultConfig(String defaultConfig) {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(defaultConfig);
        if (is == null) {
            LOGGER.error(PREFIX + "Failed to load default config file!");
            LOGGER.error(PREFIX + "Please report this to the mod author!");
            return "";
        }
        return new BufferedReader(new InputStreamReader(is))
            .lines().collect(Collectors.joining("\n"));
    }

    /**
     * Attempts to create a new config file.
     * @param configFile    the config file to create
     * @param defaultConfig the contents of the default config file
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createConfig(File configFile, String defaultConfig) {
        if (!configFile.exists()) {
            LOGGER.info(PREFIX + "Failed to find '{}'. Generating default...", configFile.getName());

            try {
                // try creating missing dirs and file
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();

            } catch (IOException e) {
                LOGGER.error(PREFIX + "Failed to create '{}'!", configFile.getName());
                LOGGER.trace(e);
                return;
            }

            try {
                // write default config data
                PrintWriter writer = new PrintWriter(configFile, StandardCharsets.UTF_8);
                writer.write(getDefaultConfig(defaultConfig));
                writer.close();

            } catch (IOException e) {
                LOGGER.error(PREFIX + "Failed to generate '{}'!", configFile.getName());
                LOGGER.trace(e);
                return;
            }

            LOGGER.info(PREFIX + "Generated new '{}'!", configFile.getName());
        }
    }

    /**
     * Attempts to load a config file.
     * @param configFile the config file to load
     */
    private void loadConfig(File configFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(configFile, StandardCharsets.UTF_8));
            int lineNum = 1;
            String line;

            boolean failed = false;
            while ((line = br.readLine()) != null) {
                failed = failed || !parseLine(line, lineNum, configFile.getName());
                lineNum++;
            }
            br.close();

            if (failed) {
                LOGGER.warn(PREFIX + "Ignoring '{}' due to syntax errors!", configFile.getName());
                config.clear();
                return;
            }

            if (config.size() == 0) {
                LOGGER.warn(PREFIX + "No keys found in '{}'! It is probably broken...", configFile.getName());
                deleteFile(configFile);
            }

        } catch (IOException e) {
            LOGGER.error(PREFIX + "Failed to load '{}'!", configFile.getName());
            LOGGER.trace(e);
        }
    }

    /**
     * Attempts to parse a line from a config file.
     * @param line the line to parse
     * @param lineNum the line number
     * @param filename the name of the config file
     * @return true if the line was parsed successfully, false otherwise
     */
    private boolean parseLine(String line, int lineNum, String filename) {
        line = line.trim().toLowerCase();
        if (line.isEmpty() || line.startsWith("#")) return true;

        String[] keyPair = line.split("=", 2);

        if (keyPair.length != 2) {
            LOGGER.warn(PREFIX + "{} line {}: Found a syntax error!", filename, lineNum);
            return false;
        }

        config.put(keyPair[0].trim(), keyPair[1].trim());
        return true;
    }

    private void deleteFile(File file) {
        try {
            if (!file.delete()) {
                LOGGER.error(PREFIX + "Failed to delete '{}'! Is it in use?", file.getName());
                return;
            }
            LOGGER.info(PREFIX + "Deleted '{}'. Restart the game to regenerate it.", file.getName());
        } catch (Exception e) {
            LOGGER.error(PREFIX + "Failed to delete '{}'!", file.getName());
            LOGGER.trace(e);
        }
    }

    /**
     * Attempt to get a key value from the config file.
     * @return the key value, or def if the key is missing.
     */
    @SuppressWarnings("unused")
    public String getOrDefault(String key, String def) {
        String val = config.get(key);
        return val == null ? def : val;
    }

    /**
     * Attempt to get a key value from the config file.
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
