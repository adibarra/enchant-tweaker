package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETUtils;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.LuckEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @description Modify enchantment level cap.
 * @note LuckEnchantment is used by: Looting, Fortune, and Luck of the Sea
 * @environment Server
 */
@Mixin(value=LuckEnchantment.class, priority=1543)
public abstract class LuckEnchantMixin extends Enchantment {

    @SuppressWarnings("unused")
    protected LuckEnchantMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    @ModifyReturnValue(method="getMaxLevel()I", at=@At("RETURN"))
    private int modifyMaxLevel(int original) {
        if(EnchantTweaker.isEnabled() && Registries.ENCHANTMENT.getKey(this).isPresent()) {
            String id = Registries.ENCHANTMENT.getKey(this).get().getValue().getPath();
            int lvl_cap = EnchantTweaker.getConfig().getOrDefault(id, original);
            if (lvl_cap == -1) return original;
            return ETUtils.clamp(lvl_cap, 0, 255);
        }
        return original;
    }
}