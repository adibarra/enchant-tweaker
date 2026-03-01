package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;
import java.util.Set;

/**
 * @description Allow specific damage types to bypass Protection enchantment entirely.
 * Configurable via comma-separated list of damage type IDs (e.g. "magic,wither,dragon_breath").
 * Supports both vanilla IDs (e.g. "magic") and modded IDs (e.g. "mymod:custom_damage").
 * @environment Server
 */
@Mixin(value=LivingEntity.class)
public abstract class ProtectionBypassMixin {

    @Unique
    private static Set<RegistryKey<DamageType>> enchanttweaker$parsedBypassTypes;

    @Unique
    private static String enchanttweaker$lastBypassConfig;

    @Unique
    private static Set<RegistryKey<DamageType>> enchanttweaker$parseBypassTypes(String config) {
        if (config.equals(enchanttweaker$lastBypassConfig) && enchanttweaker$parsedBypassTypes != null) {
            return enchanttweaker$parsedBypassTypes;
        }
        Set<RegistryKey<DamageType>> keys = new HashSet<>();
        if (config.isEmpty()) {
            enchanttweaker$lastBypassConfig = config;
            enchanttweaker$parsedBypassTypes = keys;
            return keys;
        }
        for (String entry : config.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            Identifier id = trimmed.contains(":")
                ? Identifier.tryParse(trimmed)
                : Identifier.of("minecraft", trimmed);
            if (id != null) {
                keys.add(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id));
            }
        }
        enchanttweaker$lastBypassConfig = config;
        enchanttweaker$parsedBypassTypes = keys;
        return keys;
    }

    @WrapOperation(
        method="modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/entity/DamageUtil;getInflictedDamage(FF)F"))
    private float enchanttweaker$protectionBypass$check(float damage, float epf, Operation<Float> original,
            @Local(argsOnly=true) DamageSource source) {
        if (!ETMixinPlugin.getMixinConfig("ProtectionBypassMixin")) {
            return original.call(damage, epf);
        }
        String config = ETMixinPlugin.getConfig().getOrDefault("protection_bypass_types", "");
        Set<RegistryKey<DamageType>> bypassTypes = enchanttweaker$parseBypassTypes(config);
        for (RegistryKey<DamageType> key : bypassTypes) {
            if (source.isOf(key)) {
                return damage;
            }
        }
        return original.call(damage, epf);
    }
}
