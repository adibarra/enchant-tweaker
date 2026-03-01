package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description Prevent specific enchantments from appearing in enchanting tables, loot, trades, and anvils.
 * @environment Server
 */
@Mixin(value=Enchantment.class)
public abstract class DisableEnchantmentsMixin {

    @Unique
    private static Set<String> enchanttweaker$parsedDisabledEnchants;

    @Unique
    private static String enchanttweaker$lastDisableConfig;

    @Unique
    private static Set<String> enchanttweaker$parseDisabledEnchants(String config) {
        if (config.equals(enchanttweaker$lastDisableConfig) && enchanttweaker$parsedDisabledEnchants != null) {
            return enchanttweaker$parsedDisabledEnchants;
        }
        Set<String> ids = ConcurrentHashMap.newKeySet();
        if (config.isEmpty()) {
            enchanttweaker$lastDisableConfig = config;
            enchanttweaker$parsedDisabledEnchants = ids;
            return ids;
        }
        for (String entry : config.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                ids.add(trimmed);
            }
        }
        enchanttweaker$lastDisableConfig = config;
        enchanttweaker$parsedDisabledEnchants = ids;
        return ids;
    }

    @Unique
    private boolean enchanttweaker$isDisabled() {
        if (!ETMixinPlugin.getMixinConfig("DisableEnchantmentsMixin")) return false;
        String config = ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", "");
        Set<String> disabled = enchanttweaker$parseDisabledEnchants(config);
        if (disabled.isEmpty()) return false;
        String path = ADUtils.getEnchantmentPath((Enchantment)(Object)this);
        return path != null && disabled.contains(path);
    }

    @Inject(method="isAvailableForRandomSelection()Z", at=@At("HEAD"), cancellable=true)
    private void enchanttweaker$disableEnchantments$blockRandomSelection(CallbackInfoReturnable<Boolean> cir) {
        if (enchanttweaker$isDisabled()) cir.setReturnValue(false);
    }

    @Inject(method="isAvailableForEnchantedBookOffer()Z", at=@At("HEAD"), cancellable=true)
    private void enchanttweaker$disableEnchantments$blockBookOffer(CallbackInfoReturnable<Boolean> cir) {
        if (enchanttweaker$isDisabled()) cir.setReturnValue(false);
    }

    @Inject(method="getMaxLevel()I", at=@At("HEAD"), cancellable=true)
    private void enchanttweaker$disableEnchantments$blockMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if (enchanttweaker$isDisabled()) cir.setReturnValue(0);
    }
}
