package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETMixinPlugin;
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
            case ALL -> ETMixinPlugin.getConfig().getOrDefault("protection", orig);
            case FIRE -> ETMixinPlugin.getConfig().getOrDefault("fire_protection", orig);
            case FALL -> ETMixinPlugin.getConfig().getOrDefault("feather_falling", orig);
            case EXPLOSION -> ETMixinPlugin.getConfig().getOrDefault("blast_protection", orig);
            case PROJECTILE -> ETMixinPlugin.getConfig().getOrDefault("projectile_protection", orig);
            //noinspection UnnecessaryDefault
            default -> orig;
        };
        if (lvlCap < 0) return orig;
        return Utils.clamp(lvlCap, 0, 255);
    }
}