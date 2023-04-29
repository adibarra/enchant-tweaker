package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.Utils;
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

    @ModifyReturnValue(
        method="getMaxLevel()I",
        at=@At("RETURN"))
    private int modifyMaxLevel(int orig) {
        int lvlCap = switch (this.typeIndex) {
            case 0 -> EnchantTweaker.getConfig().getOrDefault("sharpness", orig);
            case 1 -> EnchantTweaker.getConfig().getOrDefault("smite", orig);
            case 2 -> EnchantTweaker.getConfig().getOrDefault("bane_of_arthropods", orig);
            default -> orig;
        };
        if (lvlCap < 0) return orig;
        return Utils.clamp(lvlCap, 0, 255);
    }
}