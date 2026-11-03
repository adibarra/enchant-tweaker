package com.adibarra.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ADUtils {

    private static final Map<Enchantment, String> ENCHANTMENT_PATH_CACHE = new ConcurrentHashMap<>();

    private ADUtils() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * represents an inventory
     */
    public record Inventory(List<ItemStack> inv) {
        public Inventory(ItemStack inv) {
            this(List.of(inv));
        }
    }

    /**
     * gets the registry path of an enchantment (e.g. "sharpness", "mending")
     *
     * @param ench
     *            the enchantment
     * @return the path, or empty if not registered
     */
    public static @Nullable String getEnchantmentPath(Enchantment enchantment) {
        String cached = ENCHANTMENT_PATH_CACHE.get(enchantment);
        if (cached != null)
            return cached;
        String path = BuiltInRegistries.ENCHANTMENT.getResourceKey(enchantment).map(key -> key.location().getPath())
            .orElse(null);
        if (path != null)
            ENCHANTMENT_PATH_CACHE.put(enchantment, path);
        return path;
    }

    /**
     * broadcasts a message to all ops on the server
     *
     * @param server
     *            the server to broadcast to
     * @param msg
     *            the message to broadcast
     */
    public static void broadcastOps(MinecraftServer server, MutableComponent msg) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.sendSystemMessage(msg);
            }
        }
    }

    /**
     * gets a player's hotbar
     *
     * @param player
     *            the player
     * @return the hotbar inventory
     */
    public static Inventory getPlayerHotbar(Player player) {
        int size = net.minecraft.world.entity.player.Inventory.getSelectionSize();
        List<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            list.set(i, player.getInventory().getItem(i));
        }
        return new Inventory(list);
    }

    /**
     * gets an item that matches the condition from the given inventories checks
     * inventories in order, returning a random matching item from the first
     * inventory that has one
     *
     * @param inventories
     *            the inventories to search
     * @param condition
     *            the condition
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
            if (list.isEmpty())
                continue;
            return list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }
        return null;
    }

    /**
     * returns whether a binding curse roll keeps the item instead of dropping it
     */
    public static boolean bindingKeepsItem(int level, double step, float roll) {
        double dropChance = Math.clamp(1.0 + step - step * level, 0.0, 1.0);
        return roll >= dropChance;
    }

    /** returns whether an infinity roll preserves the arrow */
    public static boolean infinityPreservesArrow(int level, double percentPerLevel, float roll) {
        double consumeChance = Math.clamp(1.0 - percentPerLevel * level, 0.0, 1.0);
        return roll >= consumeChance;
    }
}
