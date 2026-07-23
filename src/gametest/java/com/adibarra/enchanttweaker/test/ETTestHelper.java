package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

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

    private static final Map<TestContext, List<ServerPlayerEntity>> CREATED_SERVER_PLAYERS = new IdentityHashMap<>();

    @SuppressWarnings("removal")
    static ServerPlayerEntity createServerPlayer(TestContext helper, GameMode mode) {
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        trackServerPlayer(helper, player);
        player.changeGameMode(mode);
        return player;
    }

    private static void trackServerPlayer(TestContext helper, ServerPlayerEntity player) {
        synchronized (CREATED_SERVER_PLAYERS) {
            List<ServerPlayerEntity> players = CREATED_SERVER_PLAYERS.get(helper);
            if (players != null) {
                players.add(player);
                return;
            }

            players = new ArrayList<>();
            List<ServerPlayerEntity> trackedPlayers = players;
            try {
                helper.addFinalTask(() -> {
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

    private static void removeServerPlayers(TestContext helper, List<ServerPlayerEntity> players) {
        PlayerManager playerManager = helper.getWorld().getServer().getPlayerManager();
        for (ServerPlayerEntity player : players) {
            if (playerManager.getPlayer(player.getUuid()) == player) {
                playerManager.remove(player);
            } else {
                ServerWorld world = player.getServerWorld();
                if (!player.isRemoved() && !world.getPlayers(entity -> entity == player).isEmpty()) {
                    // a player can have been removed from PlayerManager by a disconnect
                    // before this final task runs.
                    world.removePlayer(player, Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    /** calls the protected Enchantment.canAccept via reflection */
    static boolean canAccept(Enchantment enchantment, Enchantment other) {
        try {
            Method m = Enchantment.class.getDeclaredMethod("canAccept", Enchantment.class);
            m.setAccessible(true);
            return (boolean) m.invoke(enchantment, other);
        } catch (Exception e) {
            throw new RuntimeException("canAccept reflection failed", e);
        }
    }

    /** calls the protected Enchantment.isAcceptableItem via reflection */
    static boolean isAcceptableItem(Enchantment enchantment, ItemStack stack) {
        try {
            Method m = Enchantment.class.getDeclaredMethod("isAcceptableItem", ItemStack.class);
            m.setAccessible(true);
            return (boolean) m.invoke(enchantment, stack);
        } catch (Exception e) {
            throw new RuntimeException("isAcceptableItem reflection failed", e);
        }
    }

    /**
     * sets the two input slots of an AnvilScreenHandler by directly writing to the
     * underlying SimpleInventory stacks list, bypassing the markDirty callback
     * chain that would otherwise NPE in sendContentUpdates during tests
     */
    static void setAnvilInputs(AnvilScreenHandler handler, ItemStack first, ItemStack second) {
        try {
            Field inputField = ForgingScreenHandler.class.getDeclaredField("input");
            inputField.setAccessible(true);
            Object inv = inputField.get(handler);
            Field stacksField = null;
            for (Class<?> type = inv.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    stacksField = type.getDeclaredField("heldStacks");
                    break;
                } catch (NoSuchFieldException ignored) {
                    // continue through the inventory class hierarchy
                }
            }
            if (stacksField == null)
                throw new NoSuchFieldException("heldStacks");
            stacksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ItemStack> stacks = (List<ItemStack>) stacksField.get(inv);
            stacks.set(0, first);
            stacks.set(1, second);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setAnvilInputs reflection failed", e);
        }
    }

    /** calls static AnvilScreenHandler.getNextCost via reflection */
    static int getNextAnvilCost(int cost) {
        try {
            Method m = AnvilScreenHandler.class.getDeclaredMethod("getNextCost", int.class);
            m.setAccessible(true);
            return (int) m.invoke(null, cost);
        } catch (Exception e) {
            throw new RuntimeException("getNextCost reflection failed", e);
        }
    }

    /**
     * calls ExperienceOrbEntity.repairPlayerGears(PlayerEntity, int) via reflection
     */
    static int repairPlayerGears(ExperienceOrbEntity orb, PlayerEntity player, int amount) {
        try {
            Method m = ExperienceOrbEntity.class.getDeclaredMethod("repairPlayerGears", PlayerEntity.class, int.class);
            m.setAccessible(true);
            return (int) m.invoke(orb, player, amount);
        } catch (Exception e) {
            throw new RuntimeException("repairPlayerGears reflection failed", e);
        }
    }

    /**
     * sets the newItemName field on AnvilScreenHandler via reflection (field is
     * private)
     */
    static void setAnvilNewName(AnvilScreenHandler handler, String name) {
        try {
            Field f = AnvilScreenHandler.class.getDeclaredField("newItemName");
            f.setAccessible(true);
            f.set(handler, name);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setAnvilNewName reflection failed", e);
        }
    }

    /** calls the protected AnvilScreenHandler.onTakeOutput via reflection */
    static void invokeOnTakeOutput(AnvilScreenHandler handler, PlayerEntity player, ItemStack stack) {
        try {
            Method m = AnvilScreenHandler.class.getDeclaredMethod("onTakeOutput", PlayerEntity.class, ItemStack.class);
            m.setAccessible(true);
            m.invoke(handler, player, stack);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("onTakeOutput reflection failed", e);
        }
    }

    /** sets the two input slots of a GrindstoneScreenHandler via reflection */
    static void setGrindstoneInputs(net.minecraft.screen.GrindstoneScreenHandler handler, ItemStack first,
        ItemStack second) {
        try {
            Field inputField = net.minecraft.screen.GrindstoneScreenHandler.class.getDeclaredField("input");
            inputField.setAccessible(true);
            net.minecraft.inventory.Inventory inv = (net.minecraft.inventory.Inventory) inputField.get(handler);
            inv.setStack(0, first);
            inv.setStack(1, second);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setGrindstoneInputs reflection failed", e);
        }
    }

    /** calls GrindstoneScreenHandler.updateResult() via reflection */
    static void grindstoneUpdateResult(net.minecraft.screen.GrindstoneScreenHandler handler) {
        try {
            Method m = net.minecraft.screen.GrindstoneScreenHandler.class.getDeclaredMethod("updateResult");
            m.setAccessible(true);
            m.invoke(handler);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("grindstoneUpdateResult reflection failed", e);
        }
    }

    /** gets the result inventory from a GrindstoneScreenHandler via reflection */
    static ItemStack getGrindstoneResult(net.minecraft.screen.GrindstoneScreenHandler handler) {
        try {
            Field resultField = net.minecraft.screen.GrindstoneScreenHandler.class.getDeclaredField("result");
            resultField.setAccessible(true);
            net.minecraft.inventory.Inventory result = (net.minecraft.inventory.Inventory) resultField.get(handler);
            return result.getStack(0);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("getGrindstoneResult reflection failed", e);
        }
    }

    /**
     * calls protected LivingEntity.modifyAppliedDamage(DamageSource, float) via
     * reflection
     */
    static float modifyAppliedDamage(net.minecraft.entity.LivingEntity entity,
        net.minecraft.entity.damage.DamageSource source, float damage) {
        try {
            Method m = net.minecraft.entity.LivingEntity.class.getDeclaredMethod("modifyAppliedDamage",
                net.minecraft.entity.damage.DamageSource.class, float.class);
            m.setAccessible(true);
            return (float) m.invoke(entity, source, damage);
        } catch (Exception e) {
            throw new RuntimeException("modifyAppliedDamage reflection failed", e);
        }
    }

    static float[] snapshotRainGradient(ServerWorld world) {
        try {
            Field rg = World.class.getDeclaredField("rainGradient");
            rg.setAccessible(true);
            Field rgp = World.class.getDeclaredField("rainGradientPrev");
            rgp.setAccessible(true);
            return new float[]{rg.getFloat(world), rgp.getFloat(world)};
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("snapshotRainGradient reflection failed", e);
        }
    }

    static void restoreRainGradient(ServerWorld world, float[] gradients) {
        try {
            Field rg = World.class.getDeclaredField("rainGradient");
            rg.setAccessible(true);
            rg.setFloat(world, gradients[0]);
            Field rgp = World.class.getDeclaredField("rainGradientPrev");
            rgp.setAccessible(true);
            rgp.setFloat(world, gradients[1]);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("restoreRainGradient reflection failed", e);
        }
    }

    /**
     * forces World.rainGradient so World.isRaining() immediately reflects the
     * desired state `World.setWeather()` sets the rain flag but the gradient
     * updates lazily one tick at a time
     */
    static void forceRainGradient(ServerWorld world, float gradient) {
        try {
            Field rg = World.class.getDeclaredField("rainGradient");
            rg.setAccessible(true);
            rg.setFloat(world, gradient);
            Field rgp = World.class.getDeclaredField("rainGradientPrev");
            rgp.setAccessible(true);
            rgp.setFloat(world, gradient);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("forceRainGradient reflection failed", e);
        }
    }
}
