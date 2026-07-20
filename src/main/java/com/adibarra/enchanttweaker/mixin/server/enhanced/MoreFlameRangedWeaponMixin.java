package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.enchanttweaker.FlameLevelAccess;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures Flame from the weapon when a player-fired projectile is created. */
@Mixin(RangedWeaponItem.class)
public abstract class MoreFlameRangedWeaponMixin {

    @Inject(
        method="createArrowEntity(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/projectile/ProjectileEntity;",
        at=@At("RETURN"))
    private void enchanttweaker$moreFlame$captureWeaponLevel(
            World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack,
            boolean critical, CallbackInfoReturnable<ProjectileEntity> cir) {
        if (cir.getReturnValue() instanceof FlameLevelAccess access) {
            access.enchanttweaker$setFlameLevel(EnchantmentHelper.getLevel(Enchantments.FLAME, weaponStack));
        }
    }
}
