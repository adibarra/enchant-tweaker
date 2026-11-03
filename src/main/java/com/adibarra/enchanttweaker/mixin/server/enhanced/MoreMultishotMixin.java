package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description scales multishot arrows fired according to enchantment level
 * @environment server
 */
@Mixin(
    value = ProjectileWeaponItem.class)
public abstract class MoreMultishotMixin {

    @Unique
    private static int enchanttweaker$moreMultishot$projectileCount(int level, int perLevel) {
        long projectileCount = (long) level * Math.max(0, perLevel) + 1;
        return (int) Math.clamp(projectileCount, 1, 256);
    }

    @ModifyExpressionValue(
        method = "draw(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/List;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processProjectileCount(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;I)I"))
    private static int enchanttweaker$moreMultishot$modifyNumProjectiles(int orig, @Local(
        argsOnly = true,
        ordinal = 0) ItemStack weaponStack) {
        if (!ETMixinPlugin.getMixinConfig("MoreMultishotMixin"))
            return orig;
        int perLevel = ETMixinPlugin.getConfig().getOrDefault("more_multishot_per_level", 2);
        int multishotLevel = ADUtils.getEnchantmentLevel(weaponStack, Enchantments.MULTISHOT);
        if (multishotLevel <= 0)
            return orig;
        return enchanttweaker$moreMultishot$projectileCount(multishotLevel, perLevel);
    }
}
