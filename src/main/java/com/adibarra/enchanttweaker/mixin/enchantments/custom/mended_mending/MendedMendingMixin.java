package com.adibarra.enchanttweaker.mixin.enchantments.custom.mended_mending;

import com.adibarra.enchanttweaker.EnchantTweaker;
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

@Mixin(value=ExperienceOrbEntity.class, priority=1543)
public abstract class MendedMendingMixin {

    private int enchanttweaker$mendingLevel = 0;

    @Inject(method="repairPlayerGears(Lnet/minecraft/entity/player/PlayerEntity;I)I", at=@At("HEAD"))
    private void enchanttweaker$captureLocalMendingLevel(PlayerEntity player, int amount, CallbackInfoReturnable<Integer> cir) {
        Map.Entry<EquipmentSlot, ItemStack> entry = EnchantmentHelper.chooseEquipmentWith(Enchantments.MENDING, player, ItemStack::isDamaged);
        if (entry != null) {
            ItemStack itemStack = entry.getValue();
            enchanttweaker$mendingLevel = EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);
        }
    }

    /**
     * @author adibarra
     * @reason Changes mending enchantment repair cost depending on level
     */
    @Inject(method="getMendingRepairCost(I)I", at=@At("HEAD"), cancellable=true)
    private void enchanttweaker$mendedMending(int repairAmount, CallbackInfoReturnable<Integer> cir) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("mended_mending", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            cir.setReturnValue(repairAmount / Math.max(1,enchanttweaker$mendingLevel));
        }
    }
}