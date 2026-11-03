package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.ThornsEnchantment;
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
        method = "doPostHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;I)V",
        at = @org.spongepowered.asm.mixin.injection.At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean enchanttweaker$noThornsBacklash$skipAttackerDamage(Entity attacker, DamageSource source,
        float amount, Operation<Boolean> original) {
        if (!ETMixinPlugin.getMixinConfig("NoThornsBacklashMixin"))
            return original.call(attacker, source, amount);
        return false;
    }
}
