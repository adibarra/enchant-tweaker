package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description lets tridents with loyalty enchant return when thrown into the
 *              void
 * @environment server
 */
@Mixin(
    value = ThrownTrident.class)
public abstract class LoyalVoidTridentsMixin extends Projectile {

    @Shadow
    @Final
    private static EntityDataAccessor<Byte> ID_LOYALTY;

    @Shadow
    private boolean dealtDamage;

    @SuppressWarnings("unused")
    protected LoyalVoidTridentsMixin(EntityType<Projectile> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(
        method = "tick()V",
        at = @At("HEAD"))
    private void enchanttweaker$loyalVoidTridents$returnFromVoid(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("LoyalVoidTridentsMixin"))
            return;
        if (entityData.get(ID_LOYALTY) == 0)
            return;
        if (this.getOwner() == null)
            return;

        if (this.getY() <= this.level().getMinY()) {
            this.setPos(this.getX(), this.level().getMinY(), this.getZ());
            this.dealtDamage = true;
            this.setDeltaMovement(0, 0, 0);
        }
    }
}
