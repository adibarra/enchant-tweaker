package com.adibarra.enchanttweaker;

import com.adibarra.enchanttweaker.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class EnchantTweakerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> ETMixinPlugin.syncConfigFrom(payload.configData()));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ETMixinPlugin.restoreLocalConfig();
        });
    }
}
