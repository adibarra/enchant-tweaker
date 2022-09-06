package com.adibarra.enchanttweaker.mixin.enchantments.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ProtectionEnchantment.class, priority=1543)
public abstract class ProtectionMixin {

    /**
     * @author adibarra
     * @reason Modify enchantment level cap
     */
    @Inject(method="getMaxLevel()I", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        int protection_level_cap = EnchantTweaker.getConfig().getOrDefault("protection", -1);
        int feather_falling_level_cap = EnchantTweaker.getConfig().getOrDefault("feather_falling", -1);
        int proj_protection_level_cap = EnchantTweaker.getConfig().getOrDefault("projectile_protection", -1);
        int fire_protection_level_cap = EnchantTweaker.getConfig().getOrDefault("fire_protection", -1);
        int blast_protection_level_cap = EnchantTweaker.getConfig().getOrDefault("blast_protection", -1);

        if(EnchantTweaker.MOD_ENABLED) {
            if (((Object)this) == Enchantments.PROTECTION) {
                if (protection_level_cap != -1) {
                    cir.setReturnValue(protection_level_cap);
                }
            } else if (((Object)this) == Enchantments.FEATHER_FALLING) {
                if (feather_falling_level_cap != -1) {
                    cir.setReturnValue(feather_falling_level_cap);
                }
            } else if (((Object)this) == Enchantments.PROJECTILE_PROTECTION) {
                if (proj_protection_level_cap != -1) {
                    cir.setReturnValue(proj_protection_level_cap);
                }
            } else if (((Object)this) == Enchantments.FIRE_PROTECTION) {
                if (fire_protection_level_cap != -1) {
                    cir.setReturnValue(fire_protection_level_cap);
                }
            } else if (((Object)this) == Enchantments.BLAST_PROTECTION) {
                if (blast_protection_level_cap != -1) {
                    cir.setReturnValue(blast_protection_level_cap);
                }
            }
        }
    }
}