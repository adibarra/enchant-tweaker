package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Scales the number of the arrows fired by Multishot enchant based on its level.
 * @environment Server
 */
@Mixin(value=RangedWeaponItem.class)
public abstract class MoreMultishotMixin {

    @ModifyConstant(
        method="load(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)Ljava/util/List;",
        constant=@Constant(intValue=3))
    private static int enchanttweaker$moreMultishot$modifyNumProjectiles(int orig, ItemStack weaponStack, ItemStack projectileStack, LivingEntity shooter) {
        if (!ETMixinPlugin.getMixinConfig("MoreMultishotMixin")) return orig;
        return EnchantmentHelper.getLevel(Enchantments.MULTISHOT, weaponStack) * 2 + 1;
    }
}
