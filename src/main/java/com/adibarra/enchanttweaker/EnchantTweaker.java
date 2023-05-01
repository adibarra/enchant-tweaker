package com.adibarra.enchanttweaker;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnchantTweaker implements ModInitializer {

	public static final String MOD_NAME = "EnchantTweaker";
	public static final String MOD_ID = "enchanttweaker";
	public static final String PREFIX = "[" + MOD_NAME + "] ";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
		if (ETMixinPlugin.getConfig().getOrDefault("mod_enabled", false)) {
			ETCommands.registerCommands();
			LOGGER.info(PREFIX + "Ready to go! Applied {} Mixins.", ETMixinPlugin.getNumMixins());
		}
	}

}
