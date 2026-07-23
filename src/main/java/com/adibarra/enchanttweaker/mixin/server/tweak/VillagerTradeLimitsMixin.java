package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description limit villager enchantment trade uses and restock behavior
 * @environment server
 */
@Mixin(
    value = TradeOffer.class)
public abstract class VillagerTradeLimitsMixin {

    @Shadow
    public abstract ItemStack getSellItem();

    @Shadow
    public abstract int getUses();

    // single volatile (raw, parsed) holder swapped atomically, avoids the torn
    // reads
    // possible with two independently-published static fields
    @Unique
    private static volatile Map.Entry<String, Set<Identifier>> enchanttweaker$noRestockCache;

    @Inject(
        method = "isDisabled()Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$tradeLimits$isDisabled(CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("VillagerTradeLimitsMixin"))
            return;
        int effectiveMax = enchanttweaker$getEffectiveMaxUses(this.getSellItem());
        if (effectiveMax < 0)
            return;
        cir.setReturnValue(effectiveMax == 0 || this.getUses() >= effectiveMax);
    }

    @Inject(
        method = "resetUses()V",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$tradeLimits$resetUses(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("VillagerTradeLimitsMixin"))
            return;
        if (!enchanttweaker$shouldRestock(this.getSellItem())) {
            ci.cancel();
        }
    }

    @Unique
    private static int enchanttweaker$getEffectiveMaxUses(ItemStack sellItem) {
        ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(sellItem);
        if (enchants.isEmpty())
            return -1;

        int globalMax = ETMixinPlugin.getConfig().getOrDefault("enchant_trade_max_uses", -1);
        int effectiveMax = -1;
        for (var entry : enchants.getEnchantmentsMap()) {
            Identifier id = entry.getKey().getKey().map(k -> k.getValue()).orElse(null);
            int candidate = id == null ? globalMax : enchanttweaker$getPerEnchantMax(id, globalMax);
            if (candidate >= 0)
                effectiveMax = effectiveMax < 0 ? candidate : Math.min(effectiveMax, candidate);
        }

        return effectiveMax;
    }

    @Unique
    private static int enchanttweaker$getPerEnchantMax(Identifier id, int globalMax) {
        int namespacedMax = ETMixinPlugin.getConfig().getOrDefault("trade_" + id, -1);
        if (namespacedMax >= 0)
            return namespacedMax;
        if (Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
            int legacyMax = ETMixinPlugin.getConfig().getOrDefault("trade_" + id.getPath(), -1);
            if (legacyMax >= 0)
                return legacyMax;
        }
        return globalMax;
    }

    @Unique
    private static Set<Identifier> enchanttweaker$parseNoRestockList(String config) {
        Map.Entry<String, Set<Identifier>> cached = enchanttweaker$noRestockCache;
        if (cached != null && cached.getKey().equals(config)) {
            return cached.getValue();
        }
        Set<Identifier> names = new HashSet<>();
        if (!config.isEmpty()) {
            for (String entry : config.split(",")) {
                Identifier id = Identifier.tryParse(entry.trim());
                if (id != null)
                    names.add(id);
            }
        }
        Set<Identifier> parsed = Set.copyOf(names);
        enchanttweaker$noRestockCache = new AbstractMap.SimpleImmutableEntry<>(config, parsed);
        return parsed;
    }

    @Unique
    private static boolean enchanttweaker$shouldRestock(ItemStack sellItem) {
        ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(sellItem);
        if (enchants.isEmpty())
            return true;

        boolean globalRestock = ETMixinPlugin.getConfig().getOrDefault("enchant_trade_restock", true);
        if (!globalRestock)
            return false;

        String noRestockConfig = ETMixinPlugin.getConfig().getOrDefault("enchant_trade_no_restock", "");
        if (noRestockConfig.isEmpty())
            return true;

        Set<Identifier> noRestock = enchanttweaker$parseNoRestockList(noRestockConfig);
        for (var entry : enchants.getEnchantmentsMap()) {
            Identifier id = entry.getKey().getKey().map(k -> k.getValue()).orElse(null);
            if (id != null && noRestock.contains(id))
                return false;
        }

        return true;
    }
}
