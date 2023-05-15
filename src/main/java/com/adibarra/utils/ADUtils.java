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
     * @return the hotbar
     */
    public static List<ItemStack> getPlayerHotbar(PlayerEntity player) {
        List<ItemStack> list = DefaultedList.ofSize(PlayerInventory.getHotbarSize(), ItemStack.EMPTY);
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            list.set(i, player.getInventory().getStack(i));
        }
        return list;
    }

    /**
     * Checks if the given stack matches the condition.
     *
     * @param stack     the stack to check
     * @param condition the condition
     * @return true if the stack matches the condition, false otherwise
     */
    public static boolean isMatchingItem(ItemStack stack, Predicate<ItemStack> condition) {
        return stack != null && !stack.isEmpty() && condition.test(stack);
    }

    /**
     * Gets a list of items from the given inventory that match the condition.
     *
     * @param inventory the inventory to search
     * @param condition the condition
     * @return the matching items
     */
    public static List<ItemStack> getMatchingItems(List<ItemStack> inventory, Predicate<ItemStack> condition) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : inventory) {
            if (isMatchingItem(stack, condition)) {
                list.add(stack);
            }
        }
        return list;
    }

    /**
     * Gets a random item from the given inventory that matches the condition.
     *
     * @param inventory the inventory to search
     * @param condition the condition
     * @return the matching item, or null if none was found
     */
    public static ItemStack getMatchingItem(List<ItemStack> inventory, Predicate<ItemStack> condition) {
        List<ItemStack> list = getMatchingItems(inventory, condition);
        return list.isEmpty() ? null : list.get(new Random().nextInt(list.size()));
    }
}
