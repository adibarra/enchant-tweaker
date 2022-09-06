package com.adibarra.enchanttweaker.mixin.enchantments.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=DamageEnchantment.class, priority=1543)
public abstract class DamageMixin {

    /**
     * @author adibarra
     * @reason Modify enchantment level cap
     */
    @Inject(method="getMaxLevel()I", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        int sharpness_level_cap = EnchantTweaker.getConfig().getOrDefault("sharpness", -1);
        int smite_level_cap = EnchantTweaker.getConfig().getOrDefault("smite", -1);
        int bane_of_arthropods_level_cap = EnchantTweaker.getConfig().getOrDefault("bane_of_arthropods", -1);

        if(EnchantTweaker.MOD_ENABLED) {
            if (((Object)this) == Enchantments.SHARPNESS) {
                if (sharpness_level_cap != -1) {
                    cir.setReturnValue(sharpness_level_cap);
                }
            } else if (((Object)this) == Enchantments.SMITE) {
                if (smite_level_cap != -1) {
                    cir.setReturnValue(smite_level_cap);
                }
            } else if (((Object)this) == Enchantments.BANE_OF_ARTHROPODS) {
                if (bane_of_arthropods_level_cap != -1) {
                    cir.setReturnValue(bane_of_arthropods_level_cap);
                }
            }
        }
    }
}