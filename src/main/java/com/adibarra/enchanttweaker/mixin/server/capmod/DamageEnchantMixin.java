package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.ETUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.DamageEnchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @description Modify enchantment level cap.
 * @environment Server
 */
@Mixin(value=DamageEnchantment.class, priority=1543)
public abstract class DamageEnchantMixin {

    @Shadow @Final
    public int typeIndex;

    @ModifyReturnValue(method="getMaxLevel()I", at=@At("RETURN"))
    private int modifyMaxLevel(int original) {
        if(EnchantTweaker.isEnabled()) {
            int lvl_cap = switch (this.typeIndex) {
                case 0 -> EnchantTweaker.getConfig().getOrDefault("sharpness", original);
                case 1 -> EnchantTweaker.getConfig().getOrDefault("smite", original);
                case 2 -> EnchantTweaker.getConfig().getOrDefault("bane_of_arthropods", original);
                default -> -1;
            };
            if (lvl_cap == -1) return original;
            return ETUtils.clamp(lvl_cap, 0, 255);
        }
        return original;
    }
}