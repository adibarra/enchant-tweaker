package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description allow configured damage types to bypass protection
 * @environment server
 */
@Mixin(
    value = LivingEntity.class)
public abstract class ProtectionBypassMixin {

    // single volatile (raw, parsed) holder swapped atomically, avoids the torn
    // reads
    // possible with two independently-published static fields
    @Unique
    private static volatile Map.Entry<String, Set<ResourceKey<DamageType>>> enchanttweaker$bypassCache;

    @Unique
    private static Set<ResourceKey<DamageType>> enchanttweaker$parseBypassTypes(String config) {
        Map.Entry<String, Set<ResourceKey<DamageType>>> cached = enchanttweaker$bypassCache;
        if (cached != null && cached.getKey().equals(config)) {
            return cached.getValue();
        }
        Set<ResourceKey<DamageType>> keys = new HashSet<>();
        if (!config.isEmpty()) {
            for (String entry : config.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty())
                    continue;
                String normalized = trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
                ResourceLocation id = ResourceLocation.tryParse(normalized);
                if (id != null) {
                    keys.add(ResourceKey.create(Registries.DAMAGE_TYPE, id));
                }
            }
        }
        enchanttweaker$bypassCache = new AbstractMap.SimpleImmutableEntry<>(config, keys);
        return keys;
    }

    @WrapOperation(
        method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterMagicAbsorb(FF)F"))
    private float enchanttweaker$protectionBypass$check(float damage, float epf, Operation<Float> original, @Local(
        argsOnly = true) DamageSource source) {
        if (!ETMixinPlugin.getMixinConfig("ProtectionBypassMixin")) {
            return original.call(damage, epf);
        }
        String config = ETMixinPlugin.getConfig().getOrDefault("protection_bypass_types", "");
        Set<ResourceKey<DamageType>> bypassTypes = enchanttweaker$parseBypassTypes(config);
        for (ResourceKey<DamageType> key : bypassTypes) {
            if (source.is(key)) {
                return damage;
            }
        }
        return original.call(damage, epf);
    }
}
