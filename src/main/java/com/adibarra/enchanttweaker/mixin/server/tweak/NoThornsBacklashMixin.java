package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.enchantment.ThornsEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description disable thorns enchant armor self-damage backlash
 * @environment server
 */
@Mixin(
    value = ThornsEnchantment.class)
public abstract class NoThornsBacklashMixin {
    @WrapOperation(
        method = "onUserDamaged(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;I)V",
        at = @org.spongepowered.asm.mixin.injection.At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private boolean enchanttweaker$noThornsBacklash$skipAttackerDamage(Entity attacker, DamageSource source,
        float amount, Operation<Boolean> original) {
        if (!ETMixinPlugin.getMixinConfig("NoThornsBacklashMixin"))
            return original.call(attacker, source, amount);
        return false;
    }
}
