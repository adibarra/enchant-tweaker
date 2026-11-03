package com.adibarra.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
        if (enchantment == null)
            return null;
        String cached = ENCHANTMENT_PATH_CACHE.get(enchantment);
        if (cached != null)
            return cached;
        if (enchantment.description().getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            int lastDot = key.lastIndexOf('.');
            String path = lastDot >= 0 ? key.substring(lastDot + 1) : key;
            ENCHANTMENT_PATH_CACHE.put(enchantment, path);
            return path;
        }
        return null;
    }

    /**
     * gets the registry path of an enchantment holder (e.g. "sharpness", "mending")
     *
     * @param holder
     *            the enchantment holder
     * @return the path, or empty if not registered
     */
    public static @Nullable String getEnchantmentPath(Holder<Enchantment> holder) {
        if (holder == null)
            return null;
        return holder.unwrapKey().map(key -> key.identifier().getPath())
            .orElseGet(() -> getEnchantmentPath(holder.value()));
    }

    /**
     * checks whether an enchantment is the one identified by the given key
     *
     * @param enchantment
     *            the enchantment
     * @param key
     *            the expected enchantment key
     * @return true when the path matches
     */
    public static boolean isEnchantment(Enchantment enchantment, ResourceKey<Enchantment> key) {
        String path = getEnchantmentPath(enchantment);
        return path != null && path.equals(key.identifier().getPath());
    }

    /**
     * checks whether an enchantment holder is the one identified by the given key
     *
     * @param holder
     *            the enchantment holder
     * @param key
     *            the expected enchantment key
     * @return true when the key matches
     */
    public static boolean isEnchantment(Holder<Enchantment> holder, ResourceKey<Enchantment> key) {
        return holder != null && holder.unwrapKey().map(k -> k.equals(key)).orElse(false);
    }

    /**
     * gets the level of an enchantment on an item stack without registry access
     *
     * @param stack
     *            the item stack
     * @param key
     *            the enchantment key
     * @return the level, or 0 when absent
     */
    public static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty() || key == null)
            return 0;
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            if (entry.getKey().unwrapKey().map(k -> k.equals(key)).orElse(false))
                return entry.getIntValue();
        }
        return 0;
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
            if (server.getPlayerList().isOp(player.nameAndId())) {
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

    /**
     * blast protection knockback resistance with multiplicative scaling
     *
     * @param level
     *            the total blast protection level
     * @param configBase
     *            the configured base (clamped to 0-1)
     * @return the knockback resistance (knockback is multiplied by one minus this)
     */
    public static double blastKnockbackResistance(int level, double configBase) {
        double base = Math.clamp(configBase, 0.0, 1.0);
        return 1.0 - Math.pow(base, level);
    }
}
