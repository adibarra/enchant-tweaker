package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.MendingLevelAccess;
import com.adibarra.utils.ADUtils;

/**
 * @description scales mending efficiency based on enchantment level
 * @environment server
 */
@Mixin(
    value = EnchantmentHelper.class)
public abstract class MoreMendingMixin implements MendingLevelAccess {

    @Unique
    private int mendingLevel = 0;

    /**
     * lets the mending mixin provide its level during repair
     */
    @Unique
    public void enchanttweaker$setMendingLevel(int level) {
        this.mendingLevel = level;
    }

    @ModifyExpressionValue(
        method = "modifyDurabilityToRepairFromXp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I"))
    private static int enchanttweaker$moreMending$modifyRepairAmount(int orig, @Local(
        argsOnly = true) ItemStack itemStack) {
        if (!ETMixinPlugin.getMixinConfig("MoreMendingMixin"))
            return orig;
        int level = ADUtils.getEnchantmentLevel(itemStack, Enchantments.MENDING);
        if (level <= 0 || orig <= 0)
            return orig;
        double step = ETMixinPlugin.getConfig().getOrDefault("more_mending_step", 0.05);
        // clamps the floor before calculating the repair amount
        double floor = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_mending_floor", 0.1), 0.0, 0.6);
        // vanilla repairs 2 durability per xp (cost factor 0.5); rescale so the
        // effective xp cost matches repairAmount * clamp(0.6 - step * level, floor,
        // 0.6)
        double factor = Math.clamp(0.6 - step * level, floor, 0.6);
        double scaled = orig * (0.5 / factor);
        double nearestInteger = Math.rint(scaled);
        if (Math.abs(scaled - nearestInteger) <= Math.ulp(scaled) * 4)
            scaled = nearestInteger;
        return Math.max(1, (int) Math.floor(scaled));
    }
}
