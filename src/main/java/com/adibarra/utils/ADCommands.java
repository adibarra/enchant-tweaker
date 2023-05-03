package com.adibarra.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;

@SuppressWarnings("unused")
public class ADCommands {

    private ADCommands() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * Broadcasts a message to all ops on the server.
     *
     * @param server the server to broadcast to
     * @param msg    the message to broadcast
     */
    public static void broadcastOps(MinecraftServer server, MutableText msg) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                player.sendMessage(msg);
            }
        }
    }
}
