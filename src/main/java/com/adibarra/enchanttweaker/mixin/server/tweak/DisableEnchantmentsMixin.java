package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description prevent specific enchantments from appearing in enchanting
 *              tables, loot, trades, and anvils
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class DisableEnchantmentsMixin {

    // single volatile (raw, parsed) holder swapped atomically, avoids the torn
    // reads
    // possible with two independently-published static fields
    @Unique
    private static volatile Map.Entry<String, Set<ResourceLocation>> enchanttweaker$disableCache;

    @Unique
    private static Set<ResourceLocation> enchanttweaker$parseDisabledEnchants(String config) {
        Map.Entry<String, Set<ResourceLocation>> cached = enchanttweaker$disableCache;
        if (cached != null && cached.getKey().equals(config))
            return cached.getValue();

        Set<ResourceLocation> ids = new HashSet<>();
        for (String entry : config.split(",")) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null)
                ids.add(id);
        }
        Set<ResourceLocation> parsed = Set.copyOf(ids);
        enchanttweaker$disableCache = Map.entry(config, parsed);
        return parsed;
    }

    @Unique
    private boolean enchanttweaker$isDisabled() {
        if (!ETMixinPlugin.getMixinConfig("DisableEnchantmentsMixin"))
            return false;
        String config = ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", "");
        Set<ResourceLocation> disabled = enchanttweaker$parseDisabledEnchants(config);
        if (disabled.isEmpty())
            return false;
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey((Enchantment) (Object) this);
        return id != null && disabled.contains(id);
    }

    @Inject(
        method = "isDiscoverable()Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$disableEnchantments$blockRandomSelection(CallbackInfoReturnable<Boolean> cir) {
        if (enchanttweaker$isDisabled())
            cir.setReturnValue(false);
    }

    @Inject(
        method = "isTradeable()Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$disableEnchantments$blockBookOffer(CallbackInfoReturnable<Boolean> cir) {
        if (enchanttweaker$isDisabled())
            cir.setReturnValue(false);
    }

    @Inject(
        method = "getMaxLevel()I",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$disableEnchantments$blockMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if (enchanttweaker$isDisabled())
            cir.setReturnValue(0);
    }
}
