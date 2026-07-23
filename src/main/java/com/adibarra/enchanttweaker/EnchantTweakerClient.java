package com.adibarra.enchanttweaker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adibarra.enchanttweaker.network.ConfigSyncPayload;

@Environment(EnvType.CLIENT)
public class EnchantTweakerClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnchantTweaker.MOD_NAME);
    private static volatile long connectionGeneration;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            if (payload.version() != ConfigSyncPayload.PROTOCOL_VERSION) {
                LOGGER.warn(EnchantTweaker.PREFIX
                    + "Server sent config sync protocol v{} but this client speaks v{}, ignoring sync and keeping local config",
                    payload.version(), ConfigSyncPayload.PROTOCOL_VERSION);
                return;
            }
            long generation = connectionGeneration;
            context.client().execute(() -> {
                if (generation == connectionGeneration)
                    ETMixinPlugin.syncConfigFrom(payload.configData());
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            connectionGeneration++;
            ETMixinPlugin.captureLocalConfig();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            connectionGeneration++;
            ETMixinPlugin.restoreLocalConfig();
        });
    }
}
