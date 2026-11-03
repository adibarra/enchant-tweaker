package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description filter disabled enchantments from explicit enchant_randomly loot
 *              lists
 * @environment server
 */
@Mixin(
    value = EnchantRandomlyFunction.class)
public abstract class DisableEnchantmentsLootMixin {

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
    private static boolean enchanttweaker$isDisabled(Holder<Enchantment> holder, Set<Identifier> disabled) {
        return holder.unwrapKey().map(key -> disabled.contains(key.identifier())).orElse(false);
    }

    @ModifyVariable(
        method = "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("STORE"),
        index = 7,
        require = 1)
    private List<Holder<Enchantment>> enchanttweaker$disableEnchantments$filterLoot(List<Holder<Enchantment>> list,
        ItemStack stack, LootContext context) {
        if (!ETMixinPlugin.getMixinConfig("DisableEnchantmentsLootMixin"))
            return list;

        String rawDisabled = ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", "");
        Set<Identifier> disabled = enchanttweaker$parseDisabledEnchants(rawDisabled);
        if (disabled.isEmpty())
            return list;

        List<Holder<Enchantment>> filtered = list.stream().filter(entry -> !enchanttweaker$isDisabled(entry, disabled))
            .toList();
        return filtered.size() == list.size() ? list : filtered;
    }
}
