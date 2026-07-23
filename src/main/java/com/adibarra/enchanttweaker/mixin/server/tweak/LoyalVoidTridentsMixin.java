package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.world.World;
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
    value = TridentEntity.class)
public abstract class LoyalVoidTridentsMixin extends ProjectileEntity {

    @Shadow
    @Final
    private static TrackedData<Byte> LOYALTY;

    @Shadow
    private boolean dealtDamage;

    @SuppressWarnings("unused")
    protected LoyalVoidTridentsMixin(EntityType<ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
        method = "tick()V",
        at = @At("HEAD"))
    private void enchanttweaker$loyalVoidTridents$returnFromVoid(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("LoyalVoidTridentsMixin"))
            return;
        if (dataTracker.get(LOYALTY) == 0)
            return;
        if (this.getOwner() == null)
            return;

        if (this.getY() <= this.getWorld().getBottomY()) {
            this.setPosition(this.getX(), this.getWorld().getBottomY(), this.getZ());
            this.dealtDamage = true;
            this.setVelocity(0, 0, 0);
        }
    }
}
