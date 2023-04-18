package com.adibarra.enchanttweaker.mixin.enchant.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.utils.Utils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=ProtectionEnchantment.class, priority=1543)
public abstract class ProtectionMixin {

    @Shadow @Final
    public ProtectionEnchantment.Type protectionType;

    /**
     * @description Modify enchantment level cap.
     * @environment Server
     */
    @ModifyReturnValue(method="getMaxLevel()I", at=@At("RETURN"))
    private int modifyMaxLevel(int original) {
        if(EnchantTweaker.isEnabled()) {
            int lvl_cap = switch (this.protectionType) {
                case ALL -> EnchantTweaker.getConfig().getOrDefault("protection", original);
                case FIRE -> EnchantTweaker.getConfig().getOrDefault("fire_protection", original);
                case FALL -> EnchantTweaker.getConfig().getOrDefault("feather_falling", original);
                case EXPLOSION -> EnchantTweaker.getConfig().getOrDefault("blast_protection", original);
                case PROJECTILE -> EnchantTweaker.getConfig().getOrDefault("projectile_protection", original);
                //noinspection UnnecessaryDefault
                default -> -1;
            };
            if (lvl_cap == -1) return original;
            return Utils.clamp(lvl_cap, 0, 255);
        }
        return original;
    }
}