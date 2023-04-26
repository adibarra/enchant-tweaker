package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETUtils;
import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;

/**
 * @description Modify enchantment level cap. (Special cases, not all enchantments override getMaxLevel())
 * @environment Server
 */
@Mixin(value={
        AquaAffinityEnchantment.class, BindingCurseEnchantment.class, ChannelingEnchantment.class,
        FlameEnchantment.class,        InfinityEnchantment.class,     MendingEnchantment.class,
        MultishotEnchantment.class,    SilkTouchEnchantment.class,    VanishingCurseEnchantment.class,
}, priority=1543)
public abstract class SpecialEnchantMixin extends Enchantment {

    private final static Map<Class<?>, String> ENCHANTS = new HashMap<>();

    static {
        ENCHANTS.put(AquaAffinityEnchantment.class,   "aqua_affinity");
        ENCHANTS.put(BindingCurseEnchantment.class,   "curse_of_binding");
        ENCHANTS.put(ChannelingEnchantment.class,     "channeling");
        ENCHANTS.put(FlameEnchantment.class,          "flame");
        ENCHANTS.put(InfinityEnchantment.class,       "infinity");
        ENCHANTS.put(MendingEnchantment.class,        "mending");
        ENCHANTS.put(MultishotEnchantment.class,      "multishot");
        ENCHANTS.put(SilkTouchEnchantment.class,      "silk_touch");
        ENCHANTS.put(VanishingCurseEnchantment.class, "curse_of_vanishing");
    }

    protected SpecialEnchantMixin(Rarity weight, EnchantmentTarget target, EquipmentSlot[] slotTypes) {
        super(weight, target, slotTypes);
    }

    @Override
    public int getMaxLevel() {
        if(EnchantTweaker.isEnabled()) {
            int lvl_cap = EnchantTweaker.getConfig().getOrDefault(ENCHANTS.get(this.getClass()), 1);
            if (lvl_cap == -1) return 1;
            return ETUtils.clamp(lvl_cap, 0, 255);
        }
        return 1;
    }
}