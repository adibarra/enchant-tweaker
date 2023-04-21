package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.ETUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

/**
 * @description Modify enchantment level cap.
 * @environment Server
 */
@Mixin(value=LuckEnchantment.class, priority=1543)
public abstract class LuckMixin extends Enchantment {

    @SuppressWarnings("unused")
    protected LuckMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    private final static Map<Enchantment, String> ENCHANTS = new HashMap<>();

    static {
        ENCHANTS.put(Enchantments.LOOTING,         "looting");
        ENCHANTS.put(Enchantments.FORTUNE,         "fortune");
        ENCHANTS.put(Enchantments.LUCK_OF_THE_SEA, "luck_of_the_sea");
    }

    @ModifyReturnValue(method="getMaxLevel()I", at=@At("RETURN"))
    private int modifyMaxLevel(int original) {
        if(EnchantTweaker.isEnabled()) {
            int lvl_cap = EnchantTweaker.getConfig().getOrDefault(ENCHANTS.get(this), original);
            if (lvl_cap == -1) return original;
            return ETUtils.clamp(lvl_cap, 0, 255);
        }
        return original;
    }
}