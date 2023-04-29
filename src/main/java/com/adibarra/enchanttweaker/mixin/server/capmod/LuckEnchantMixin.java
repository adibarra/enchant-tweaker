package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.Utils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.LuckEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.registry.Registry;
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

    @ModifyReturnValue(
        method="getMaxLevel()I",
        at=@At("RETURN"))
    private int modifyMaxLevel(int orig) {
        if (Registry.ENCHANTMENT.getKey(this).isPresent()) {
            String id = Registry.ENCHANTMENT.getKey(this).get().getValue().getPath();
            int lvlCap = ETMixinPlugin.getConfig().getOrDefault(id, orig);
            if (lvlCap < 0) return orig;
            return Utils.clamp(lvlCap, 0, 255);
        }
        return orig;
    }
}