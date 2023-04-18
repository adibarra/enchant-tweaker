package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PersistentProjectileEntity.class, priority=1543)
public abstract class MoreFlameDecoderMixin {

    private int flameLevel = 1;

    /**
     * @description Capture the flame level of the bow for later use and set the fire ticks to the correct value.
     * @environment Server
     */
    @Inject(method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", at=@At("HEAD"))
    private void calculateFlameLevel(EntityHitResult entityHitResult, CallbackInfo ci) {
        if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("more_flame", true)) {
            Entity arrow = ((Entity) (Object) this);
            if(arrow.getFireTicks() > 0) {
                flameLevel = Math.round((float) Math.ceil(arrow.getFireTicks() / 2000F));
                arrow.setFireTicks(arrow.getFireTicks() / flameLevel);
            }
        }
    }

    /**
     * @description Use the flame level to change the burn time of the flame enchantment.
     * @environment Server
     */
    @ModifyConstant(method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", constant=@Constant(intValue=5))
    private int decoder(int original) {
        if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("more_flame", true)) {
            return flameLevel * original;
        }
        return original;
    }
}
