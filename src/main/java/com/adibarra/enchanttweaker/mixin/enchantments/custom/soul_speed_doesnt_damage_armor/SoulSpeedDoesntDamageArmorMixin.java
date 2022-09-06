package com.adibarra.enchanttweaker.mixin.enchantments.custom.soul_speed_doesnt_damage_armor;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value=LivingEntity.class, priority=1543)
public abstract class SoulSpeedDoesntDamageArmorMixin {

    /**
     * @author adibarra
     * @reason Disable damage soul speed does to own armor
     */
    @ModifyConstant(method="addSoulSpeedBoostIfNeeded()V", constant=@Constant(intValue=1))
    private int enchanttweaker$soulSpeedDoesntDamageArmor(int original) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("soul_speed_doesnt_damage_armor", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            return 0;
        }
        return original;
    }
}