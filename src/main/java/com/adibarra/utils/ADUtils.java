package com.adibarra.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class ADUtils {

    private ADUtils() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * Represents an inventory.
     */
    public record Inventory(List<ItemStack> inv) {
        public Inventory(ItemStack inv) {
            this(List.of(inv));
        }
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

    /**
     * Gets a player's hotbar.
     *
     * @param player the player
     * @return the hotbar inventory
     */
    public static Inventory getPlayerHotbar(PlayerEntity player) {
        int size = PlayerInventory.getHotbarSize();
        List<ItemStack> list = DefaultedList.ofSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            list.set(i, player.getInventory().getStack(i));
        }
        return new Inventory(list);
    }

    /**
     * Gets an item that matches the condition from the given inventories.
     * Checks inventories in order, returning a random matching item from the first inventory that has one.
     *
     * @param inventories the inventories to search
     * @param condition the condition
     * @return the matching item, or null if none was found
     */
    public static ItemStack getMatchingItem(List<Inventory> inventories, Predicate<ItemStack> condition) {
        for (Inventory inventory : inventories) {
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack stack : inventory.inv()) {
                if (stack != null && !stack.isEmpty() && condition.test(stack)) {
                    list.add(stack);
                }
            }
            if (list.isEmpty()) continue;
            return list.get(new Random().nextInt(list.size()));
        }
        return null;
    }
}
