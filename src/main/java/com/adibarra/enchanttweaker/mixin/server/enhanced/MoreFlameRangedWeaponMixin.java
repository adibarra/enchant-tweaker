package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.FlameLevelAccess;
import com.adibarra.utils.ADUtils;

/** captures flame from the weapon for player-fired projectiles */
@Mixin(ProjectileWeaponItem.class)
public abstract class MoreFlameRangedWeaponMixin {

    @Inject(
        method = "createProjectile(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/projectile/Projectile;",
        at = @At("RETURN"))
    private void enchanttweaker$moreFlame$captureWeaponLevel(Level world, LivingEntity shooter, ItemStack weaponStack,
        ItemStack projectileStack, boolean critical, CallbackInfoReturnable<Projectile> cir) {
        if (cir.getReturnValue() instanceof FlameLevelAccess access) {
            access.enchanttweaker$setFlameLevel(ADUtils.getEnchantmentLevel(weaponStack, Enchantments.FLAME));
        }
    }
}
