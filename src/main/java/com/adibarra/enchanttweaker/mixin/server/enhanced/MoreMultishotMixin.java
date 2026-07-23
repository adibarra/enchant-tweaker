package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description scales multishot arrows fired according to enchantment level
 * @environment server
 */
@Mixin(
    value = RangedWeaponItem.class)
public abstract class MoreMultishotMixin {

    @Unique
    private static int enchanttweaker$moreMultishot$projectileCount(int level, int perLevel) {
        long projectileCount = (long) level * Math.max(0, perLevel) + 1;
        return (int) Math.clamp(projectileCount, 1, 256);
    }

    @ModifyConstant(
        method = "load(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)Ljava/util/List;",
        constant = @Constant(
            intValue = 3,
            ordinal = 0))
    private static int enchanttweaker$moreMultishot$modifyNumProjectiles(int orig, ItemStack weaponStack,
        ItemStack projectileStack, LivingEntity shooter) {
        if (!ETMixinPlugin.getMixinConfig("MoreMultishotMixin"))
            return orig;
        int perLevel = ETMixinPlugin.getConfig().getOrDefault("more_multishot_per_level", 2);
        int multishotLevel = EnchantmentHelper.getLevel(Enchantments.MULTISHOT, weaponStack);
        return enchanttweaker$moreMultishot$projectileCount(multishotLevel, perLevel);
    }
}
