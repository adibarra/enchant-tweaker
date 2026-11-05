package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description make mending and unbreaking enchantments mutually exclusive
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class NoMendingUnbreakingMixin {

    @ModifyReturnValue(
        method = "areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z",
        at = @At("RETURN"))
    private static boolean enchanttweaker$noMendingUnbreaking$blockCoexist(boolean original, Holder<Enchantment> first,
        Holder<Enchantment> second) {
        if (!ETMixinPlugin.getMixinConfig("NoMendingUnbreakingMixin"))
            return original;
        if ((first.is(Enchantments.MENDING) && second.is(Enchantments.UNBREAKING))
            || (first.is(Enchantments.UNBREAKING) && second.is(Enchantments.MENDING))) {
            return false;
        }
        return original;
    }
}
