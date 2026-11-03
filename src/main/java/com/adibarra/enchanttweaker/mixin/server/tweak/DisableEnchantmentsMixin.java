package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;
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
    private static volatile Map.Entry<String, Set<Identifier>> enchanttweaker$disableCache;

    @Unique
    private static Set<Identifier> enchanttweaker$parseDisabledEnchants(String config) {
        Map.Entry<String, Set<Identifier>> cached = enchanttweaker$disableCache;
        if (cached != null && cached.getKey().equals(config))
            return cached.getValue();

        Set<Identifier> ids = new HashSet<>();
        for (String entry : config.split(",")) {
            Identifier id = Identifier.tryParse(entry.trim());
            if (id != null)
                ids.add(id);
        }
        Set<Identifier> parsed = Set.copyOf(ids);
        enchanttweaker$disableCache = Map.entry(config, parsed);
        return parsed;
    }

    @Unique
    private @Nullable Identifier enchanttweaker$getIdentifier() {
        if (((Enchantment) (Object) this).description().getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            // vanilla keys are "enchantment.<namespace>.<path>"
            if (key.startsWith("enchantment.")) {
                String rest = key.substring("enchantment.".length());
                int dot = rest.indexOf('.');
                if (dot > 0 && dot < rest.length() - 1) {
                    return Identifier.fromNamespaceAndPath(rest.substring(0, dot), rest.substring(dot + 1));
                }
            }
        }
        return null;
    }

    @Unique
    private boolean enchanttweaker$isDisabled() {
        if (!ETMixinPlugin.getMixinConfig("DisableEnchantmentsMixin"))
            return false;
        String config = ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", "");
        Set<Identifier> disabled = enchanttweaker$parseDisabledEnchants(config);
        if (disabled.isEmpty())
            return false;
        Identifier id = enchanttweaker$getIdentifier();
        return id != null && disabled.contains(id);
    }

    @Inject(
        method = "getMaxLevel()I",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$disableEnchantments$blockMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if (enchanttweaker$isDisabled())
            cir.setReturnValue(0);
    }

    @Inject(
        method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$disableEnchantments$blockAnvil(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (enchanttweaker$isDisabled())
            cir.setReturnValue(false);
    }

    @Inject(
        method = "isSupportedItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$disableEnchantments$blockEnchanting(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (enchanttweaker$isDisabled())
            cir.setReturnValue(false);
    }

    @Inject(
        method = "isPrimaryItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$disableEnchantments$blockRandomSelection(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (enchanttweaker$isDisabled())
            cir.setReturnValue(false);
    }
}
