package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.world.item.enchantment.DamageEnchantment;
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
    value = DamageEnchantment.class)
public abstract class GodWeaponsMixin {

    @Inject(
        method = "checkCompatibility(Lnet/minecraft/world/item/enchantment/Enchantment;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$godWeapons$allowAllDamageEnchants(Enchantment other,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("GodWeaponsMixin"))
            return;
        if (other instanceof DamageEnchantment) {
            cir.setReturnValue((Object) this != other);
        }
    }
}
