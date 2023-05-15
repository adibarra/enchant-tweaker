package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
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
    private int enchanttweaker$damageEnchant$modifyMaxLevel(int orig) {
        int lvlCap = switch (this.typeIndex) {
            case 0 -> ETMixinPlugin.getConfig().getOrDefault("sharpness", orig);
            case 1 -> ETMixinPlugin.getConfig().getOrDefault("smite", orig);
            case 2 -> ETMixinPlugin.getConfig().getOrDefault("bane_of_arthropods", orig);
            default -> orig;
        };
        if (lvlCap < 0) return orig;
        return ADMath.clamp(lvlCap, 0, 255);
    }
}
