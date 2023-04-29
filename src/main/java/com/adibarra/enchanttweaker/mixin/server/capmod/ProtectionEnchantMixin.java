package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.Utils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @description Modify enchantment level cap.
 * @environment Server
 */
@Mixin(value=ProtectionEnchantment.class, priority=1543)
public abstract class ProtectionEnchantMixin {

    @Shadow @Final
    public ProtectionEnchantment.Type protectionType;

    @ModifyReturnValue(
        method="getMaxLevel()I",
        at=@At("RETURN"))
    private int modifyMaxLevel(int orig) {
        int lvlCap = switch (this.protectionType) {
            case ALL -> EnchantTweaker.getConfig().getOrDefault("protection", orig);
            case FIRE -> EnchantTweaker.getConfig().getOrDefault("fire_protection", orig);
            case FALL -> EnchantTweaker.getConfig().getOrDefault("feather_falling", orig);
            case EXPLOSION -> EnchantTweaker.getConfig().getOrDefault("blast_protection", orig);
            case PROJECTILE -> EnchantTweaker.getConfig().getOrDefault("projectile_protection", orig);
            //noinspection UnnecessaryDefault
            default -> orig;
        };
        if (lvlCap < 0) return orig;
        return Utils.clamp(lvlCap, 0, 255);
    }
}