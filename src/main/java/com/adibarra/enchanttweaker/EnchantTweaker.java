package com.adibarra.enchanttweaker;

import com.adibarra.utils.Utils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EnchantTweaker implements ModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger("EnchantTweaker");

    @Override
    public void onInitialize() {
		if (ETMixinPlugin.getConfig().getOrDefault("mod_enabled", true)) {
			ETCommands.registerCommands();
			LOGGER.info("Enchant Tweaker is ready to go! Applied {} Mixins.", ETMixinPlugin.getNumMixins());
		}
	}

	public static String getDefaultConfig(String filename) {
		try {
			return Utils.getResourceFileAsString(filename + ".properties");
		} catch (IOException e) {
			LOGGER.error("Failed to load default config file! Report this error here: https://github.com/adibarra/enchant-tweaker/issues", e);
			throw new RuntimeException(e);
		}
    }
}
