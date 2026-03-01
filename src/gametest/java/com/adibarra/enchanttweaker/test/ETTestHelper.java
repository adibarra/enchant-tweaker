package com.adibarra.enchanttweaker.test;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

class ETTestHelper {

    static void setCapmod(boolean enabled) {
        ETMixinPlugin.getConfig().set("capmod_enabled", String.valueOf(enabled));
        ETMixinPlugin.clearCaches();
    }

    static void setEnchantCap(String key, int level) {
        ETMixinPlugin.getConfig().set(key, String.valueOf(level));
        ETMixinPlugin.clearCaches();
    }

    static void setFeature(String key, boolean on) {
        ETMixinPlugin.getConfig().set(key, String.valueOf(on));
        ETMixinPlugin.clearCaches();
    }

    /** Calls the protected Enchantment.canAccept via reflection. */
    static boolean canAccept(Enchantment enchantment, Enchantment other) {
        try {
            Method m = Enchantment.class.getDeclaredMethod("canAccept", Enchantment.class);
            m.setAccessible(true);
            return (boolean) m.invoke(enchantment, other);
        } catch (Exception e) {
            throw new RuntimeException("canAccept reflection failed", e);
        }
    }

    /** Calls the protected Enchantment.isAcceptableItem via reflection. */
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
     * Sets the two input slots of an AnvilScreenHandler by directly writing to the
     * underlying SimpleInventory stacks list, bypassing the markDirty callback chain
     * that would otherwise NPE in sendContentUpdates during tests.
     */
    static void setAnvilInputs(AnvilScreenHandler handler, ItemStack first, ItemStack second) {
        try {
            Field inputField = ForgingScreenHandler.class.getDeclaredField("input");
            inputField.setAccessible(true);
            Object inv = inputField.get(handler);
            Field stacksField = inv.getClass().getSuperclass().getDeclaredField("heldStacks");
            stacksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ItemStack> stacks = (List<ItemStack>) stacksField.get(inv);
            stacks.set(0, first);
            stacks.set(1, second);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setAnvilInputs reflection failed", e);
        }
    }

    /** Calls static AnvilScreenHandler.getNextCost via reflection. */
    static int getNextAnvilCost(int cost) {
        try {
            Method m = AnvilScreenHandler.class.getDeclaredMethod("getNextCost", int.class);
            m.setAccessible(true);
            return (int) m.invoke(null, cost);
        } catch (Exception e) {
            throw new RuntimeException("getNextCost reflection failed", e);
        }
    }

    /** Calls ExperienceOrbEntity.repairPlayerGears(PlayerEntity, int) via reflection. */
    static int repairPlayerGears(ExperienceOrbEntity orb, PlayerEntity player, int amount) {
        try {
            Method m = ExperienceOrbEntity.class.getDeclaredMethod("repairPlayerGears", PlayerEntity.class, int.class);
            m.setAccessible(true);
            return (int) m.invoke(orb, player, amount);
        } catch (Exception e) {
            throw new RuntimeException("repairPlayerGears reflection failed", e);
        }
    }

    /** Sets the newItemName field on AnvilScreenHandler via reflection (field is private). */
    static void setAnvilNewName(AnvilScreenHandler handler, String name) {
        try {
            Field f = AnvilScreenHandler.class.getDeclaredField("newItemName");
            f.setAccessible(true);
            f.set(handler, name);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("setAnvilNewName reflection failed", e);
        }
    }

    /** Calls the protected AnvilScreenHandler.onTakeOutput via reflection. */
    static void invokeOnTakeOutput(AnvilScreenHandler handler, PlayerEntity player, ItemStack stack) {
        try {
            Method m = AnvilScreenHandler.class.getDeclaredMethod("onTakeOutput", PlayerEntity.class, ItemStack.class);
            m.setAccessible(true);
            m.invoke(handler, player, stack);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("onTakeOutput reflection failed", e);
        }
    }

    /**
     * Forces World.rainGradient so World.isRaining() immediately reflects the desired state.
     * World.setWeather() sets the rain flag but the gradient updates lazily one tick at a time.
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
