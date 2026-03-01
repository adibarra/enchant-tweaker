package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * @description Limit villager enchantment trade uses and restock behavior.
 * @environment Server
 */
@Mixin(value=TradeOffer.class)
public abstract class VillagerTradeLimitsMixin {

    @Shadow public abstract ItemStack getSellItem();
    @Shadow public abstract int getUses();

    @Unique
    private static Set<String> enchanttweaker$parsedNoRestock;

    @Unique
    private static String enchanttweaker$lastNoRestockConfig;

    @Inject(
        method="isDisabled()Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$tradeLimits$isDisabled(CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("VillagerTradeLimitsMixin")) return;
        int effectiveMax = enchanttweaker$getEffectiveMaxUses(this.getSellItem());
        if (effectiveMax < 0) return;
        if (effectiveMax == 0 || this.getUses() >= effectiveMax) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method="resetUses()V",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$tradeLimits$resetUses(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("VillagerTradeLimitsMixin")) return;
        if (!enchanttweaker$shouldRestock(this.getSellItem())) {
            ci.cancel();
        }
    }

    @Unique
    private static int enchanttweaker$getEffectiveMaxUses(ItemStack sellItem) {
        ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(sellItem);
        if (enchants.isEmpty()) return -1;

        int globalMax = ETMixinPlugin.getConfig().getOrDefault("enchant_trade_max_uses", -1);
        int effectiveMax = globalMax;

        for (var entry : enchants.getEnchantmentsMap()) {
            String path = entry.getKey().getKey().map(k -> k.getValue().getPath()).orElse(null);
            if (path == null) continue;
            int perEnchantMax = ETMixinPlugin.getConfig().getOrDefault("trade_" + path, -1);
            if (perEnchantMax == 0) return 0;
            if (perEnchantMax > 0) {
                effectiveMax = (effectiveMax < 0) ? perEnchantMax : Math.min(effectiveMax, perEnchantMax);
            }
        }

        return effectiveMax;
    }

    @Unique
    private static Set<String> enchanttweaker$parseNoRestockList(String config) {
        if (config.equals(enchanttweaker$lastNoRestockConfig) && enchanttweaker$parsedNoRestock != null) {
            return enchanttweaker$parsedNoRestock;
        }
        Set<String> names = new HashSet<>();
        if (!config.isEmpty()) {
            for (String entry : config.split(",")) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) names.add(trimmed);
            }
        }
        enchanttweaker$lastNoRestockConfig = config;
        enchanttweaker$parsedNoRestock = names;
        return names;
    }

    @Unique
    private static boolean enchanttweaker$shouldRestock(ItemStack sellItem) {
        ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(sellItem);
        if (enchants.isEmpty()) return true;

        boolean globalRestock = ETMixinPlugin.getConfig().getOrDefault("enchant_trade_restock", true);
        if (!globalRestock) return false;

        String noRestockConfig = ETMixinPlugin.getConfig().getOrDefault("enchant_trade_no_restock", "");
        if (noRestockConfig.isEmpty()) return true;

        Set<String> noRestock = enchanttweaker$parseNoRestockList(noRestockConfig);
        for (var entry : enchants.getEnchantmentsMap()) {
            String path = entry.getKey().getKey().map(k -> k.getValue().getPath()).orElse(null);
            if (path != null && noRestock.contains(path)) return false;
        }

        return true;
    }
}
