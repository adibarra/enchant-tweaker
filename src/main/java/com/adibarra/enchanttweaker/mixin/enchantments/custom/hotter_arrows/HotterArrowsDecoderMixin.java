package com.adibarra.enchanttweaker.mixin.enchantments.custom.hotter_arrows;

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
public abstract class HotterArrowsDecoderMixin {

    private int enchanttweaker$flameLevel;

    /**
     * @author adibarra
     * @reason Changes flame enchantment burn time depending on level
     */
    @Inject(method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", at=@At("HEAD"))
    private void enchanttweaker$calculateFlameLevel(EntityHitResult entityHitResult, CallbackInfo ci) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("hotter_arrows", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            Entity arrow = ((Entity) (Object) this);
            if(arrow.getFireTicks() > 0) {
                enchanttweaker$flameLevel = Math.round((float) Math.ceil(arrow.getFireTicks() / 2000F));
                arrow.setFireTicks(arrow.getFireTicks() / enchanttweaker$flameLevel);
            }
        }
    }

    @ModifyConstant(method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", constant=@Constant(intValue=5))
    private int enchanttweaker$HotterArrowsDecoder(int original) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("hotter_arrows", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            return enchanttweaker$flameLevel * original;
        }
        return original;
    }
}
