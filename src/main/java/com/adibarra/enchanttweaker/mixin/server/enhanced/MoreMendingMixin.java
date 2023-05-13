package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * @description Scales the efficiency of the Mending enchant based on its level.
 * @environment Server
 */
@Mixin(value=ExperienceOrbEntity.class, priority=1543)
public abstract class MoreMendingMixin {

    private int mendingLevel = 0;

    @Inject(
        method="repairPlayerGears(Lnet/minecraft/entity/player/PlayerEntity;I)I",
        at=@At(
            value="INVOKE_ASSIGN",
            target="Lnet/minecraft/enchantment/EnchantmentHelper;chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"))
    private void captureMendingLevel(PlayerEntity player, int amount, CallbackInfoReturnable<Integer> cir, @Local Map.Entry<EquipmentSlot, ItemStack> entry) {
        if (entry != null) {
            mendingLevel = EnchantmentHelper.getLevel(Enchantments.MENDING, entry.getValue());
        }
    }

    @Inject(
        method="getMendingRepairCost(I)I",
        at=@At("HEAD"),
        cancellable=true)
    private void moreMending(int repairAmount, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) Math.round(repairAmount * Math.max(0.6 - 0.05 * mendingLevel, 0.1)));
    }
}
