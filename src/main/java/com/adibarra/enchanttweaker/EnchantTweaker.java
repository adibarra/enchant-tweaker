package com.adibarra.enchanttweaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adibarra.enchanttweaker.network.ConfigSyncPayload;

public class EnchantTweaker implements ModInitializer {

    public static final String MOD_NAME = "EnchantTweaker";
    public static final String MOD_ID = "enchanttweaker";
    public static final String PREFIX = "[" + MOD_NAME + "] ";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.getPlayer(), ConfigSyncPayload.ID)) {
                ServerPlayNetworking.send(handler.getPlayer(), new ConfigSyncPayload(ETMixinPlugin.getConfigMap()));
            }
        });

        if (ETMixinPlugin.getConfig().getOrDefault("mod_enabled", false)) {
            ETCommands.registerCommands();
            ETCommands.registerEventListeners();
            AnvilRepairHandler.register();

            LOGGER.info(PREFIX + "Ready to go! Applied {} Mixins.", ETMixinPlugin.getNumMixins());
        }
    }
}
