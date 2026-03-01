package com.adibarra.enchanttweaker.mixin.server.grindstone;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.GrindstoneDisenchantAccess;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @description Handle grindstone output slot behavior for disenchanting to books.
 * @environment Server
 */
@Mixin(targets="net.minecraft.screen.GrindstoneScreenHandler$4")
public abstract class GrindstoneOutputSlotMixin {

    @Shadow @Final
    GrindstoneScreenHandler field_16780; // parent handler reference

    @Inject(
        method="onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$grindstoneDisenchant$onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("GrindstoneDisenchantMixin")) return;

        int bookSlot = ((GrindstoneDisenchantAccess)field_16780).enchanttweaker$getBookSlot();
        if (bookSlot < 0) return; // Not a disenchant operation, let vanilla handle

        Inventory input = ((GrindstoneDisenchantAccess)field_16780).enchanttweaker$getInput();
        int enchantedSlot = bookSlot == 0 ? 1 : 0;
        ItemStack enchantedItem = input.getStack(enchantedSlot);
        boolean keepItem = ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant_keep_item", true);

        if (keepItem && !enchantedItem.isEmpty()) {
            // Create clean version of the enchanted item
            ItemStack cleanItem;
            if (enchantedItem.isOf(Items.ENCHANTED_BOOK)) {
                // For book splitting: remove the extracted enchantment, keep the rest
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(enchantedItem);
                ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                boolean skippedFirst = false;
                for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentsMap()) {
                    if (!entry.getKey().value().isCursed() && !skippedFirst) {
                        skippedFirst = true; // Skip the first non-curse (it was extracted)
                        continue;
                    }
                    builder.add(entry.getKey().value(), entry.getIntValue());
                }
                ItemEnchantmentsComponent remaining = builder.build();
                if (remaining.isEmpty()) {
                    cleanItem = new ItemStack(Items.BOOK);
                } else {
                    cleanItem = enchantedItem.copy();
                    EnchantmentHelper.set(cleanItem, remaining);
                }
            } else {
                // Regular item: strip all non-curse enchantments
                cleanItem = enchantedItem.copy();
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(cleanItem);
                ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentsMap()) {
                    if (entry.getKey().value().isCursed()) {
                        builder.add(entry.getKey().value(), entry.getIntValue());
                    }
                }
                EnchantmentHelper.set(cleanItem, builder.build());
            }

            // Return clean item to player
            if (!player.getInventory().insertStack(cleanItem)) {
                player.dropItem(cleanItem, false);
            }
        }

        // Consume both inputs
        input.setStack(0, ItemStack.EMPTY);
        input.setStack(1, ItemStack.EMPTY);

        // Skip vanilla onTakeItem (no XP spawned)
        ci.cancel();
    }
}
