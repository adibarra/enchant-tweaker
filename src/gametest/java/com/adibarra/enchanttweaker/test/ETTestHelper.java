package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import com.adibarra.enchanttweaker.ETMixinPlugin;

class ETTestHelper {

    static void setCapmod(boolean enabled) {
        setConfigValue("capmod_enabled", String.valueOf(enabled));
    }

    static void setEnchantCap(String key, int level) {
        setConfigValue(key, String.valueOf(level));
    }

    static void setFeature(String key, boolean on) {
        setConfigValue(key, String.valueOf(on));
    }

    static void setConfigValue(String key, String value) {
        ETMixinPlugin.getConfig().setAll(java.util.Map.of(key, value));
        ETMixinPlugin.clearCaches();
    }

    static Map<String, String> snapshotConfig(String... keys) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : keys) {
            String value = ETMixinPlugin.getConfig().getOrDefault(key, null);
            if (value == null)
                throw new IllegalArgumentException("missing config key: " + key);
            values.put(key, value);
        }
        return values;
    }

    static void restoreConfig(Map<String, String> values) {
        ETMixinPlugin.getConfig().setAll(values);
        ETMixinPlugin.clearCaches();
    }

    private static final Map<GameTestHelper, List<ServerPlayer>> CREATED_SERVER_PLAYERS = new IdentityHashMap<>();

    @SuppressWarnings("removal")
    static ServerPlayer createServerPlayer(GameTestHelper helper, GameType mode) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        trackServerPlayer(helper, player);
        player.setGameMode(mode);
        return player;
    }

    private static void trackServerPlayer(GameTestHelper helper, ServerPlayer player) {
        synchronized (CREATED_SERVER_PLAYERS) {
            List<ServerPlayer> players = CREATED_SERVER_PLAYERS.get(helper);
            if (players != null) {
                players.add(player);
                return;
            }

            players = new ArrayList<>();
            List<ServerPlayer> trackedPlayers = players;
            try {
                helper.succeedIf(() -> {
                    try {
                        removeServerPlayers(helper, trackedPlayers);
                    } finally {
                        synchronized (CREATED_SERVER_PLAYERS) {
                            CREATED_SERVER_PLAYERS.remove(helper);
                        }
                    }
                });
                CREATED_SERVER_PLAYERS.put(helper, players);
                players.add(player);
                return;
            } catch (IllegalStateException exception) {
                if (!"This test already has final clause".equals(exception.getMessage()))
                    throw exception;
            }
        }

        // final tasks cannot be registered after the test's final clause
        removeServerPlayers(helper, List.of(player));
    }

    private static void removeServerPlayers(GameTestHelper helper, List<ServerPlayer> players) {
        PlayerList playerManager = helper.getLevel().getServer().getPlayerList();
        for (ServerPlayer player : players) {
            if (playerManager.getPlayer(player.getUUID()) == player) {
                playerManager.remove(player);
            } else {
                ServerLevel world = (ServerLevel) player.level();
                if (!player.isRemoved() && !world.getPlayers(entity -> entity == player).isEmpty()) {
                    // a player can have been removed from PlayerManager by a disconnect
                    // before this final task runs.
                    world.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    /** checks mutual exclusivity via the static compatibility API */
    static boolean canAccept(Holder<Enchantment> first, Holder<Enchantment> second) {
        return Enchantment.areCompatible(first, second);
    }

    /** checks item support via the instance compatibility API */
    static boolean isAcceptableItem(Enchantment enchantment, ItemStack stack) {
        return enchantment.canEnchant(stack);
    }

    /**
     * sets the two input slots of an AnvilScreenHandler by directly writing to the
     * underlying SimpleInventory stacks list, bypassing the markDirty callback
     * chain that would otherwise NPE in sendContentUpdates during tests
     */
    static void setAnvilInputs(AnvilMenu handler, ItemStack first, ItemStack second) {
        try {
            Field inputField = ItemCombinerMenu.class.getDeclaredField("inputSlots");
            inputField.setAccessible(true);
            Object inv = inputField.get(handler);
            Field stacksField = null;
            for (Class<?> type = inv.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    stacksField = type.getDeclaredField("items");
                    break;
                } catch (NoSuchFieldException ignored) {
                    // continue through the inventory class hierarchy
                }
            }
            if (stacksField == null)
                throw new NoSuchFieldException("items");
            stacksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            NonNullList<ItemStack> stacks = (NonNullList<ItemStack>) stacksField.get(inv);
            stacks.set(0, first);
            stacks.set(1, second);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setAnvilInputs reflection failed", e);
        }
    }

    /** returns the increased repair cost for the next anvil operation */
    static int getNextAnvilCost(int cost) {
        return AnvilMenu.calculateIncreasedRepairCost(cost);
    }

    /**
     * calls ExperienceOrb.repairPlayerItems(ServerPlayer, int) via reflection
     */
    static int repairPlayerGears(ExperienceOrb orb, Player player, int amount) {
        try {
            Method m = ExperienceOrb.class.getDeclaredMethod("repairPlayerItems",
                net.minecraft.server.level.ServerPlayer.class, int.class);
            m.setAccessible(true);
            return (int) m.invoke(orb, player, amount);
        } catch (Exception e) {
            throw new RuntimeException("repairPlayerGears reflection failed", e);
        }
    }

    /**
     * sets the itemName field on AnvilMenu via reflection (field is private)
     */
    static void setAnvilNewName(AnvilMenu handler, String name) {
        try {
            Field f = AnvilMenu.class.getDeclaredField("itemName");
            f.setAccessible(true);
            f.set(handler, name);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setAnvilNewName reflection failed", e);
        }
    }

    /** calls the protected AnvilMenu.onTake via reflection */
    static void invokeOnTakeOutput(AnvilMenu handler, Player player, ItemStack stack) {
        try {
            Method m = AnvilMenu.class.getDeclaredMethod("onTake", Player.class, ItemStack.class);
            m.setAccessible(true);
            m.invoke(handler, player, stack);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("onTakeOutput reflection failed", e);
        }
    }

    /** sets the two repair slots of a GrindstoneMenu via reflection */
    static void setGrindstoneInputs(net.minecraft.world.inventory.GrindstoneMenu handler, ItemStack first,
        ItemStack second) {
        try {
            Field inputField = net.minecraft.world.inventory.GrindstoneMenu.class.getDeclaredField("repairSlots");
            inputField.setAccessible(true);
            net.minecraft.world.Container inv = (net.minecraft.world.Container) inputField.get(handler);
            inv.setItem(0, first);
            inv.setItem(1, second);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setGrindstoneInputs reflection failed", e);
        }
    }

    /** calls GrindstoneMenu.createResult() via reflection */
    static void grindstoneUpdateResult(net.minecraft.world.inventory.GrindstoneMenu handler) {
        try {
            Method m = net.minecraft.world.inventory.GrindstoneMenu.class.getDeclaredMethod("createResult");
            m.setAccessible(true);
            m.invoke(handler);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("grindstoneUpdateResult reflection failed", e);
        }
    }

    /** gets the result inventory from a GrindstoneMenu via reflection */
    static ItemStack getGrindstoneResult(net.minecraft.world.inventory.GrindstoneMenu handler) {
        try {
            Field resultField = net.minecraft.world.inventory.GrindstoneMenu.class.getDeclaredField("resultSlots");
            resultField.setAccessible(true);
            net.minecraft.world.Container result = (net.minecraft.world.Container) resultField.get(handler);
            return result.getItem(0);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("getGrindstoneResult reflection failed", e);
        }
    }

    /**
     * calls protected LivingEntity.getDamageAfterMagicAbsorb(DamageSource, float)
     * via reflection
     */
    static float modifyAppliedDamage(net.minecraft.world.entity.LivingEntity entity,
        net.minecraft.world.damagesource.DamageSource source, float damage) {
        try {
            Method m = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("getDamageAfterMagicAbsorb",
                net.minecraft.world.damagesource.DamageSource.class, float.class);
            m.setAccessible(true);
            return (float) m.invoke(entity, source, damage);
        } catch (Exception e) {
            throw new RuntimeException("modifyAppliedDamage reflection failed", e);
        }
    }

    static float[] snapshotRainGradient(ServerLevel world) {
        try {
            Field rg = Level.class.getDeclaredField("rainLevel");
            rg.setAccessible(true);
            Field rgp = Level.class.getDeclaredField("oRainLevel");
            rgp.setAccessible(true);
            return new float[]{rg.getFloat(world), rgp.getFloat(world)};
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("snapshotRainGradient reflection failed", e);
        }
    }

    static void restoreRainGradient(ServerLevel world, float[] gradients) {
        try {
            Field rg = Level.class.getDeclaredField("rainLevel");
            rg.setAccessible(true);
            rg.setFloat(world, gradients[0]);
            Field rgp = Level.class.getDeclaredField("oRainLevel");
            rgp.setAccessible(true);
            rgp.setFloat(world, gradients[1]);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("restoreRainGradient reflection failed", e);
        }
    }

    /**
     * forces Level.rainLevel so Level.isRaining() immediately reflects the desired
     * state `ServerLevel.setWeather()` sets the rain flag but the level updates
     * lazily one tick at a time
     */
    static void forceRainGradient(ServerLevel world, float gradient) {
        try {
            Field rg = Level.class.getDeclaredField("rainLevel");
            rg.setAccessible(true);
            rg.setFloat(world, gradient);
            Field rgp = Level.class.getDeclaredField("oRainLevel");
            rgp.setAccessible(true);
            rgp.setFloat(world, gradient);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("forceRainGradient reflection failed", e);
        }
    }
}
