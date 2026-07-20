package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description allow specific damage types to bypass Protection enchantment
 *              entirely configurable via comma-separated list of damage type
 *              IDs (e.g. "magic,wither,dragon_breath") supports both vanilla
 *              IDs (e.g. "magic") and modded IDs (e.g. "mymod:custom_damage")
 * @environment Server
 */
@Mixin(
    value = LivingEntity.class)
public abstract class ProtectionBypassMixin {

    // single volatile (raw, parsed) holder swapped atomically, avoids the torn
    // reads
    // possible with two independently-published static fields
    @Unique
    private static volatile Map.Entry<String, Set<RegistryKey<DamageType>>> enchanttweaker$bypassCache;

    @Unique
    private static Set<RegistryKey<DamageType>> enchanttweaker$parseBypassTypes(String config) {
        Map.Entry<String, Set<RegistryKey<DamageType>>> cached = enchanttweaker$bypassCache;
        if (cached != null && cached.getKey().equals(config)) {
            return cached.getValue();
        }
        Set<RegistryKey<DamageType>> keys = new HashSet<>();
        if (!config.isEmpty()) {
            for (String entry : config.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty())
                    continue;
                String normalized = trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
                Identifier id = Identifier.tryParse(normalized);
                if (id != null) {
                    keys.add(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id));
                }
            }
        }
        enchanttweaker$bypassCache = new AbstractMap.SimpleImmutableEntry<>(config, keys);
        return keys;
    }

    @WrapOperation(
        method = "modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/DamageUtil;getInflictedDamage(FF)F"))
    private float enchanttweaker$protectionBypass$check(float damage, float epf, Operation<Float> original, @Local(
        argsOnly = true) DamageSource source) {
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
