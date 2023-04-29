package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.utils.Utils;
import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.registry.Registry;
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
        int orig = super.getMaxLevel();
        if (Registry.ENCHANTMENT.getKey(this).isPresent()) {
            String id = Registry.ENCHANTMENT.getKey(this).get().getValue().getPath();
            int lvlCap = EnchantTweaker.getConfig().getOrDefault(id, orig);
            if (lvlCap < 0) return orig;
            return Utils.clamp(lvlCap, 0, 255);
        }
        return orig;
    }
}