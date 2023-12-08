package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @description Lets Channeling enchant work in rain at level 2.
 * @environment Server
 */
@Mixin(value=TridentEntity.class)
public abstract class MoreChannelingMixin extends PersistentProjectileEntity {

    // VERSION CHANGES:
    // 1.16+:
    /*
    @Shadow
    private ItemStack tridentStack;
    @SuppressWarnings("unused")
    protected MoreChannelingMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyExpressionValue(
        method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V",
        at=@At(
            ordinal=0,
            value="INVOKE",
            target="Lnet/minecraft/world/World;isThundering()Z"))
    private boolean enchanttweaker$moreChanneling$modifyOnHit(boolean orig) {
        boolean isChannelingII = EnchantmentHelper.getLevel(Enchantments.CHANNELING, tridentStack) > 1;
        return orig || (isChannelingII && this.getWorld().isRaining());
    }
    */

    // 1.20.3+:
    @SuppressWarnings("unused")
    protected MoreChannelingMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world, ItemStack stack) {
        super(entityType, world, stack);
    }

    @ModifyExpressionValue(
        method="onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V",
        at=@At(
            ordinal=0,
            value="INVOKE",
            target="Lnet/minecraft/world/World;isThundering()Z"))
    private boolean enchanttweaker$moreChanneling$modifyOnHit(boolean orig) {
        boolean isChannelingII = EnchantmentHelper.getLevel(Enchantments.CHANNELING, this.getItemStack()) > 1;
        return orig || (isChannelingII && this.getWorld().isRaining());
    }
}
