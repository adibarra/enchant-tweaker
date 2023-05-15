package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.utils.ADMath;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @description Scales the Binding Curse enchantment to have a chance of not dropping the item on death.
 * @environment Server
 */
@Mixin(value=PlayerInventory.class, priority=1543)
public abstract class MoreBindingMixin {

    private static final Random RAND = new Random();

    private static final Map<Integer, ItemStack> BOUND_ARMOR = new HashMap<>();
    static {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            for (Map.Entry<Integer, ItemStack> c : BOUND_ARMOR.entrySet()) {
                newPlayer.getInventory().armor.set(c.getKey(), c.getValue());
            }
            BOUND_ARMOR.clear();
        });
    }

    @Shadow @Final
    public DefaultedList<ItemStack> armor;

    @ModifyExpressionValue(
        method="dropAll()V",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/item/ItemStack;isEmpty()Z"))
        private boolean enchanttweaker$moreBinding$modifyDropAll(boolean orig, @Local ItemStack stack) {
        if (orig) return true;
        if (!armor.contains(stack)) return false;

        int bindingLevel = EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, stack);
        if (RAND.nextFloat() > ADMath.clamp(1.1 - 0.1 * bindingLevel, 0.1, 1.0)) {
            BOUND_ARMOR.put(armor.indexOf(stack), stack.copy());
            return true;
        }
        return false;
    }
}
