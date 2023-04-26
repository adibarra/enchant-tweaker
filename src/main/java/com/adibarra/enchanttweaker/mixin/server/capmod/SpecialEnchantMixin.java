package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETUtils;
import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;

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

    @SuppressWarnings("unused")
    protected SpecialEnchantMixin(Rarity weight, EnchantmentTarget target, EquipmentSlot[] slotTypes) {
        super(weight, target, slotTypes);
    }

    @Override
    public int getMaxLevel() {
        int original = super.getMaxLevel();
        if(EnchantTweaker.isEnabled() && Registries.ENCHANTMENT.getKey(this).isPresent()) {
            String id = Registries.ENCHANTMENT.getKey(this).get().getValue().getPath();
            int lvl_cap = EnchantTweaker.getConfig().getOrDefault(id, original);
            if (lvl_cap == -1) return original;
            return ETUtils.clamp(lvl_cap, 0, 255);
        }
        return original;
    }
}