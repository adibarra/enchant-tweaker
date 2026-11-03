package com.adibarra.enchanttweaker.mixin.server.enhanced;

import java.util.Map;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.MendingLevelAccess;

/**
 * @description scales mending efficiency based on enchantment level
 * @environment server
 */
@Mixin(
    value = ExperienceOrb.class)
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

    @Inject(
        method = "repairPlayerItems(Lnet/minecraft/world/entity/player/Player;I)I",
        at = @At(
            ordinal = 0,
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"))
    private void enchanttweaker$moreMending$captureMendingLevel(Player player, int amount,
        CallbackInfoReturnable<Integer> cir, @Local Map.Entry<EquipmentSlot, ItemStack> entry) {
        if (!ETMixinPlugin.getMixinConfig("MoreMendingMixin"))
            return;
        if (entry != null) {
            mendingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, entry.getValue());
        } else {
            mendingLevel = 0; // clears stale levels when no gear is found
        }
    }

    @Inject(
        method = "durabilityToXp(I)I",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$moreMending$modifyRepairCost(int repairAmount, CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("MoreMendingMixin"))
            return;
        double step = ETMixinPlugin.getConfig().getOrDefault("more_mending_step", 0.05);
        // clamps the floor before calculating the repair cost
        double floor = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_mending_floor", 0.1), 0.0, 0.6);
        double scaledCost = repairAmount * Math.clamp(0.6 - step * mendingLevel, floor, 0.6);
        double nearestInteger = Math.rint(scaledCost);
        if (Math.abs(scaledCost - nearestInteger) <= Math.ulp(scaledCost) * 4)
            scaledCost = nearestInteger;
        cir.setReturnValue((int) Math.floor(scaledCost));
    }
}
