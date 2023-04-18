package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @description Lets Channeling enchant work in rain at level 2.
 * @environment Server
 */
@Mixin(value=TridentEntity.class, priority=1543)
public abstract class MoreChannelingMixin extends PersistentProjectileEntity {

    @Shadow
    private ItemStack tridentStack;

    protected MoreChannelingMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyExpressionValue(method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", at=@At(value="INVOKE", target="Lnet/minecraft/world/World;isThundering()Z"))
    private boolean moreChanneling(boolean original) {
        if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("more_channeling", true)) {
            if(EnchantmentHelper.getLevel(Enchantments.CHANNELING, this.tridentStack) > 1) {
                return (original || this.world.isRaining());
            }
        }
        return original;
    }
}