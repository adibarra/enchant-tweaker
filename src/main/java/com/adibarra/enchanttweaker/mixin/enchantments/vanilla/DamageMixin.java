package com.adibarra.enchanttweaker.mixin.enchantments.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.utils.Utils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.DamageEnchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=DamageEnchantment.class, priority=1543)
public abstract class DamageMixin {

    @Shadow @Final
    public int typeIndex;

    /**
     * @description Modify enchantment level cap
     * @environment Server
     */
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
            return Utils.clamp(lvl_cap, 0, 255);
        }
        return original;
    }
}