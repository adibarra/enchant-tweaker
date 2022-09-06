package com.adibarra.enchanttweaker.mixin.enchantments.custom.loyal_tridents_fear_the_void;

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
public abstract class LoyalTridentsFearTheVoidMixin extends Entity {

    @Final
    @Shadow
    private static TrackedData<Byte> LOYALTY;

    @SuppressWarnings("unused")
    @Shadow
    private boolean dealtDamage;

    protected LoyalTridentsFearTheVoidMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * @author adibarra
     * @implNote Changes channeling enchantment behavior depending on level
     */
    @Override
    protected void tickInVoid() {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("loyal_tridents_fear_the_void", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            if(dataTracker.get(LOYALTY) > 0) {
                dealtDamage = true;
                return;
            }
        }
        super.tickInVoid();
    }
}