package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.EnchantTweaker;
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
@Mixin(value=TridentEntity.class, priority=1543)
public abstract class LoyalVoidTridentsMixin extends ProjectileEntity {

    @Final @Shadow
    private static TrackedData<Byte> LOYALTY;

    @SuppressWarnings("unused")
    @Shadow
    private boolean dealtDamage;

    @SuppressWarnings("unused")
    protected LoyalVoidTridentsMixin(EntityType<ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method="tick", at=@At("HEAD"))
    private void tick(CallbackInfo ci) {
        if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("loyal_tridents_void", true)) {
            if(dataTracker.get(LOYALTY) > 0 && !this.dealtDamage) {
                if(this.getY() <= this.world.getBottomY()) {
                    this.dealtDamage = true;
                    this.setVelocity(0, 0, 0);
                }
            }
        }
    }
}