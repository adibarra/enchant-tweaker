package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow weapons to be enchanted with multiple damage enchantments.
 * @environment Server
 */
@Mixin(value=DamageEnchantment.class)
public abstract class GodWeaponsMixin {

    @Shadow @Final
    public int typeIndex;

    @Inject(
        method="canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$godWeapons$allowAllDamageEnchants(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof DamageEnchantment damageEnchantment) {
            cir.setReturnValue(this.typeIndex != damageEnchantment.typeIndex);
        }
    }
}
