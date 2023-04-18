package com.adibarra.enchanttweaker.mixin.enchantments.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.utils.Utils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

@Mixin(value={
        AquaAffinityEnchantment.class, BindingCurseEnchantment.class, ChannelingEnchantment.class,
        DepthStriderEnchantment.class, EfficiencyEnchantment.class,   FireAspectEnchantment.class,
        FlameEnchantment.class,        FrostWalkerEnchantment.class,  ImpalingEnchantment.class,
        InfinityEnchantment.class,     KnockbackEnchantment.class,    LoyaltyEnchantment.class,
        LureEnchantment.class,         MendingEnchantment.class,      MultishotEnchantment.class,
        PiercingEnchantment.class,     PowerEnchantment.class,        PunchEnchantment.class,
        QuickChargeEnchantment.class,  RespirationEnchantment.class,  RiptideEnchantment.class,
        SilkTouchEnchantment.class,    SoulSpeedEnchantment.class,    SweepingEnchantment.class,
        SwiftSneakEnchantment.class,   ThornsEnchantment.class,       UnbreakingEnchantment.class,
        VanishingCurseEnchantment.class
}, priority=1543)
public abstract class GenericMixin {

    private final static Map<Class<?>, String> ENCHANTS = new HashMap<>();

    static {
        ENCHANTS.put(AquaAffinityEnchantment.class,   "aqua_affinity");
        ENCHANTS.put(BindingCurseEnchantment.class,   "curse_of_binding");
        ENCHANTS.put(ChannelingEnchantment.class,     "channeling");
        ENCHANTS.put(DepthStriderEnchantment.class,   "depth_strider");
        ENCHANTS.put(EfficiencyEnchantment.class,     "efficiency");
        ENCHANTS.put(FireAspectEnchantment.class,     "fire_aspect");
        ENCHANTS.put(FlameEnchantment.class,          "flame");
        ENCHANTS.put(FrostWalkerEnchantment.class,    "frost_walker");
        ENCHANTS.put(ImpalingEnchantment.class,       "impaling");
        ENCHANTS.put(InfinityEnchantment.class,       "infinity");
        ENCHANTS.put(KnockbackEnchantment.class,      "knockback");
        ENCHANTS.put(LoyaltyEnchantment.class,        "loyalty");
        ENCHANTS.put(LureEnchantment.class,           "lure");
        ENCHANTS.put(MendingEnchantment.class,        "mending");
        ENCHANTS.put(MultishotEnchantment.class,      "multishot");
        ENCHANTS.put(PiercingEnchantment.class,       "piercing");
        ENCHANTS.put(PowerEnchantment.class,          "power");
        ENCHANTS.put(PunchEnchantment.class,          "punch");
        ENCHANTS.put(QuickChargeEnchantment.class,    "quick_charge");
        ENCHANTS.put(RespirationEnchantment.class,    "respiration");
        ENCHANTS.put(RiptideEnchantment.class,        "riptide");
        ENCHANTS.put(SilkTouchEnchantment.class,      "silk_touch");
        ENCHANTS.put(SoulSpeedEnchantment.class,      "soul_speed");
        ENCHANTS.put(SweepingEnchantment.class,       "sweeping_edge");
        ENCHANTS.put(SwiftSneakEnchantment.class,     "swift_sneak");
        ENCHANTS.put(ThornsEnchantment.class,         "thorns");
        ENCHANTS.put(UnbreakingEnchantment.class,     "unbreaking");
        ENCHANTS.put(VanishingCurseEnchantment.class, "curse_of_vanishing");
    }

    /**
     * @description Modify enchantment level cap
     * @environment Server
     */
    @ModifyReturnValue(method="getMaxLevel()I", at=@At("RETURN"))
    private int modifyMaxLevel(int original) {
        if(EnchantTweaker.isEnabled()) {
            int lvl_cap = EnchantTweaker.getConfig().getOrDefault(ENCHANTS.get(this.getClass()), original);
            if (lvl_cap == -1) return original;
            return Utils.clamp(lvl_cap, 0, 255);
        }
        return original;
    }
}