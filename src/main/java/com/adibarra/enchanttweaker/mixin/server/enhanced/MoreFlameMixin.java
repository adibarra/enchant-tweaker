package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @description Scales the burn time of the Flame enchant based on its level.
 * Adds 2 seconds of burn time per level above 1.
 * @environment Server
 */
@Mixin(value=PersistentProjectileEntity.class, priority=1543)
public abstract class MoreFlameMixin {

    private int flameLevel = 0;

    @Inject(
        method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V",
        at=@At("HEAD"))
    private void captureFlameLevel(EntityHitResult entityHitResult, CallbackInfo ci) {
        flameLevel = EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, (LivingEntity) entityHitResult.getEntity());
    }

    @ModifyConstant(
        method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V",
        constant=@Constant(intValue=5))
    private int decoder(int orig) {
        return 2 * (flameLevel - 1) + orig;
    }
}
