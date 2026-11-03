package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description enables channeling during rain at level 2
 * @environment server
 */
@Mixin(
    value = ThrownTrident.class)
public abstract class MoreChannelingMixin extends AbstractArrow {

    @SuppressWarnings("unused")
    protected MoreChannelingMixin(EntityType<? extends AbstractArrow> entityType, Level world, ItemStack stack) {
        super(entityType, world, stack);
    }

    @ModifyExpressionValue(
        method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
        at = @At(
            ordinal = 0,
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isThundering()Z"))
    private boolean enchanttweaker$moreChanneling$modifyOnHit(boolean orig) {
        if (!ETMixinPlugin.getMixinConfig("MoreChannelingMixin"))
            return orig;
        boolean isChannelingII = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.CHANNELING,
            this.getPickupItemStackOrigin()) > 1;
        return orig || (isChannelingII && this.level().isRainingAt(this.blockPosition()));
    }
}
