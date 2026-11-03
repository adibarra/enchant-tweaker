package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.Optional;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
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
    value = EnchantRandomlyFunction.class)
public abstract class DisableEnchantmentsLootMixin {

    @Shadow
    @Final
    private Optional<HolderSet<Enchantment>> enchantments;

    @ModifyVariable(
        method = "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("STORE"),
        index = 4,
        require = 1)
    private Optional<Holder<Enchantment>> enchanttweaker$disableEnchantments$filterExplicitLoot(
        Optional<Holder<Enchantment>> selected, ItemStack stack, LootContext context) {
        if (!ETMixinPlugin.getMixinConfig("DisableEnchantmentsLootMixin") || enchantments.isEmpty()
            || enchantments.get().size() == 0 || selected.isEmpty()) {
            return selected;
        }

        String rawDisabled = ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", "");
        if (rawDisabled.isEmpty() || !enchanttweaker$isDisabled(selected.get().value(), rawDisabled))
            return selected;

        return Util.getRandomSafe(enchantments.get().stream()
            .filter(entry -> !enchanttweaker$isDisabled(entry.value(), rawDisabled)).toList(), context.getRandom());
    }

    private static boolean enchanttweaker$isDisabled(Enchantment enchantment, String rawDisabled) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        if (id == null)
            return false;
        for (String configured : rawDisabled.split(",")) {
            if (id.equals(ResourceLocation.tryParse(configured.trim())))
                return true;
        }
        return false;
    }
}
