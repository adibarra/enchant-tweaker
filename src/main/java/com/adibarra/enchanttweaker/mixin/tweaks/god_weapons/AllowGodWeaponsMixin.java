package com.adibarra.enchanttweaker.mixin.tweaks.god_weapons;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=DamageEnchantment.class, priority=1543)
public abstract class AllowGodWeaponsMixin {

    /**
     * @author adibarra
     * @reason Allow multiple DamageEnchantment types to overlap
     */
    @Inject(method="canAccept", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$allowGodWeapons(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("allow_god_weapons", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled && other instanceof DamageEnchantment) {
            cir.setReturnValue(true);
        }
    }
}