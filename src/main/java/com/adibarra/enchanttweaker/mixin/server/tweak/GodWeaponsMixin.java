package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description allow weapons to be enchanted with multiple damage enchantments
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class GodWeaponsMixin {

    @Inject(
        method = "areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private static void enchanttweaker$godWeapons$allowAllDamageEnchants(Holder<Enchantment> first,
        Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("GodWeaponsMixin"))
            return;
        if (!first.equals(second) && first.is(EnchantmentTags.DAMAGE_EXCLUSIVE)
            && second.is(EnchantmentTags.DAMAGE_EXCLUSIVE)) {
            cir.setReturnValue(true);
        }
    }
}
