package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

/**
 * @description Modify enchantment level cap.
 * @environment Server
 */
@Mixin(value={
    DepthStriderEnchantment.class, EfficiencyEnchantment.class,  FireAspectEnchantment.class,
    FrostWalkerEnchantment.class,  ImpalingEnchantment.class,    KnockbackEnchantment.class,
    LoyaltyEnchantment.class,      LureEnchantment.class,        PiercingEnchantment.class,
    PowerEnchantment.class,        PunchEnchantment.class,       QuickChargeEnchantment.class,
    RespirationEnchantment.class,  RiptideEnchantment.class,     SoulSpeedEnchantment.class,
    SweepingEnchantment.class,     SwiftSneakEnchantment.class,  ThornsEnchantment.class,
    UnbreakingEnchantment.class
}, priority=1543)
public abstract class GenericEnchantMixin {

    private final static Map<Class<?>, String> ENCHANTS = new HashMap<>();

    static {
        ENCHANTS.put(ChannelingEnchantment.class,     "channeling");
        ENCHANTS.put(DepthStriderEnchantment.class,   "depth_strider");
        ENCHANTS.put(EfficiencyEnchantment.class,     "efficiency");
        ENCHANTS.put(FireAspectEnchantment.class,     "fire_aspect");
        ENCHANTS.put(FrostWalkerEnchantment.class,    "frost_walker");
        ENCHANTS.put(ImpalingEnchantment.class,       "impaling");
        ENCHANTS.put(KnockbackEnchantment.class,      "knockback");
        ENCHANTS.put(LoyaltyEnchantment.class,        "loyalty");
        ENCHANTS.put(LureEnchantment.class,           "lure");
        ENCHANTS.put(PiercingEnchantment.class,       "piercing");
        ENCHANTS.put(PowerEnchantment.class,          "power");
        ENCHANTS.put(PunchEnchantment.class,          "punch");
        ENCHANTS.put(QuickChargeEnchantment.class,    "quick_charge");
        ENCHANTS.put(RespirationEnchantment.class,    "respiration");
        ENCHANTS.put(RiptideEnchantment.class,        "riptide");
        ENCHANTS.put(SoulSpeedEnchantment.class,      "soul_speed");
        ENCHANTS.put(SweepingEnchantment.class,       "sweeping_edge");
        ENCHANTS.put(SwiftSneakEnchantment.class,     "swift_sneak");
        ENCHANTS.put(ThornsEnchantment.class,         "thorns");
        ENCHANTS.put(UnbreakingEnchantment.class,     "unbreaking");
    }

    @ModifyReturnValue(
        method="getMaxLevel()I",
        at=@At("RETURN"))
    private int enchanttweaker$genericEnchant$modifyMaxLevel(int orig) {
        int lvlCap = ETMixinPlugin.getConfig().getOrDefault(ENCHANTS.get(this.getClass()), orig);
        if (lvlCap < 0) return orig;
        return ADMath.clamp(lvlCap, 0, 255);
    }
}
