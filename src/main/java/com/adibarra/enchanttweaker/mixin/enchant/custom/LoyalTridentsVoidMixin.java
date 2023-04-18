package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value=TridentEntity.class, priority=1543)
public abstract class LoyalTridentsVoidMixin extends Entity {

    @Final
    @Shadow
    private static TrackedData<Byte> LOYALTY;

    @Shadow
    private boolean dealtDamage;

    protected LoyalTridentsVoidMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * @description Makes tridents with loyalty enchantment return to the player when thrown into the void.
     * @environment Server
     */
    @Override
    protected void tickInVoid() {
        if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("loyal_tridents_void", true)) {
            if(dataTracker.get(LOYALTY) > 0) {
                dealtDamage = true;
                return;
            }
        }
        super.tickInVoid();
    }
}