package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
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
@Mixin(value=LuckEnchantment.class)
public abstract class LuckEnchantMixin extends Enchantment {

    @SuppressWarnings("unused")
    protected LuckEnchantMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    // VERSION CHANGES:
    // 1.19+: Registry
    // 1.20+: Registries
    @ModifyReturnValue(
        method="getMaxLevel()I",
        at=@At("RETURN"))
    private int enchanttweaker$luckEnchant$modifyMaxLevel(int orig) {
        if (Registries.ENCHANTMENT.getKey(this).isEmpty()) return orig;

        String key = Registries.ENCHANTMENT.getKey(this).get().getValue().getPath();
        int lvlCap = ETMixinPlugin.getConfig().getOrDefault(key, orig);
        if (lvlCap < 0) return orig;
        return ADMath.clamp(lvlCap, 0, 255);
    }
}
