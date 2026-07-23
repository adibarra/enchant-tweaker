package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.Optional;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description filter disabled enchantments from explicit enchant_randomly loot
 *              lists
 * @environment server
 */
@Mixin(
    value = EnchantRandomlyLootFunction.class)
public abstract class DisableEnchantmentsLootMixin {

    @Shadow
    @Final
    private Optional<RegistryEntryList<Enchantment>> enchantments;

    @ModifyVariable(
        method = "process(Lnet/minecraft/item/ItemStack;Lnet/minecraft/loot/context/LootContext;)Lnet/minecraft/item/ItemStack;",
        at = @At("STORE"),
        index = 4,
        require = 1)
    private Optional<RegistryEntry<Enchantment>> enchanttweaker$disableEnchantments$filterExplicitLoot(
        Optional<RegistryEntry<Enchantment>> selected, ItemStack stack, LootContext context) {
        if (!ETMixinPlugin.getMixinConfig("DisableEnchantmentsLootMixin") || enchantments.isEmpty()
            || enchantments.get().size() == 0 || selected.isEmpty()) {
            return selected;
        }

        String rawDisabled = ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", "");
        if (rawDisabled.isEmpty() || !enchanttweaker$isDisabled(selected.get().value(), rawDisabled))
            return selected;

        return Util.getRandomOrEmpty(enchantments.get().stream()
            .filter(entry -> !enchanttweaker$isDisabled(entry.value(), rawDisabled)).toList(), context.getRandom());
    }

    private static boolean enchanttweaker$isDisabled(Enchantment enchantment, String rawDisabled) {
        Identifier id = Registries.ENCHANTMENT.getId(enchantment);
        if (id == null)
            return false;
        for (String configured : rawDisabled.split(",")) {
            if (id.equals(Identifier.tryParse(configured.trim())))
                return true;
        }
        return false;
    }
}
