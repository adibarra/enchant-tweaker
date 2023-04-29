package com.adibarra.enchanttweaker;

import com.adibarra.utils.Utils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class EnchantTweaker implements ModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger("EnchantTweaker");

    @Override
    public void onInitialize() {
		if (ETMixinPlugin.getConfig().getOrDefault("mod_enabled", false)) {
			ETCommands.registerCommands();
			LOGGER.info("Enchant Tweaker is ready to go! Applied {} Mixins.", ETMixinPlugin.getNumMixins());
		}
	}

	public static String getDefaultConfig(String filename) {
		try {
			Path path = Path.of("assets", "enchanttweaker", filename + ".properties");
			return Utils.getResourceFileAsString(path.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
}
