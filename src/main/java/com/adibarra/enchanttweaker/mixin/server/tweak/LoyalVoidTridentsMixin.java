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

/**
 * @description Lets tridents with Loyalty enchant return when thrown into the void.
 * @environment Server
 */
@Mixin(value=TridentEntity.class)
public abstract class LoyalVoidTridentsMixin extends ProjectileEntity {

    @Shadow @Final
    private static TrackedData<Byte> LOYALTY;

    @Shadow
    private boolean dealtDamage;

    // VERSION CHANGES:
    // 1.16+: PersistentProjectileEntity
    // 1.20+: ProjectileEntity
    @SuppressWarnings("unused")
    protected LoyalVoidTridentsMixin(EntityType<ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(
        method="tick()V",
        at=@At("HEAD"))
    private void enchanttweaker$loyalVoidTridents$returnFromVoid(CallbackInfo ci) {
        if (dataTracker.get(LOYALTY) == 0 || this.dealtDamage) return;

        if (this.getY() <= this.getWorld().getBottomY()) {
            this.dealtDamage = true;
            this.setVelocity(0, 0, 0);
        }
    }
}
