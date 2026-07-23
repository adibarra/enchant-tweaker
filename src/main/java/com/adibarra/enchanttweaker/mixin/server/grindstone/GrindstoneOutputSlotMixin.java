package com.adibarra.enchanttweaker.mixin.server.grindstone;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
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

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.GrindstoneDisenchantAccess;

/**
 * @description handle grindstone output for disenchanting into books
 * @environment server
 */
@Mixin(
    targets = "net.minecraft.screen.GrindstoneScreenHandler$4")
public abstract class GrindstoneOutputSlotMixin {

    @Shadow
    @Final
    GrindstoneScreenHandler field_16780; // parent handler reference

    @Inject(
        method = "onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$grindstoneDisenchant$onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("GrindstoneDisenchantMixin"))
            return;

        int bookSlot = ((GrindstoneDisenchantAccess) field_16780).enchanttweaker$getBookSlot();
        if (bookSlot < 0)
            return; // not a disenchant operation, let vanilla handle

        Inventory input = ((GrindstoneDisenchantAccess) field_16780).enchanttweaker$getInput();
        int enchantedSlot = bookSlot == 0 ? 1 : 0;
        ItemStack enchantedItem = input.getStack(enchantedSlot);
        boolean keepItem = ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant_keep_item", true);

        if (keepItem && !enchantedItem.isEmpty()) {
            // create a clean enchanted item
            ItemStack cleanItem;
            if (enchantedItem.isOf(Items.ENCHANTED_BOOK)) {
                // split books by removing the extracted enchantment
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(enchantedItem);
                Enchantment extracted = null;
                for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentsMap()) {
                    if (!entry.getKey().value().isCursed()) {
                        extracted = entry.getKey().value();
                        break;
                    }
                }
                ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchants);
                if (extracted != null) {
                    Enchantment extractedEnchantment = extracted;
                    builder.remove(enchantment -> enchantment.value() == extractedEnchantment);
                }
                ItemEnchantmentsComponent remaining = builder.build();
                if (remaining.isEmpty()) {
                    cleanItem = enchantedItem.copyComponentsToNewStack(Items.BOOK, enchantedItem.getCount());
                    cleanItem.remove(DataComponentTypes.REPAIR_COST);
                    cleanItem.remove(DataComponentTypes.STORED_ENCHANTMENTS);
                    cleanItem.remove(DataComponentTypes.ENCHANTMENTS);
                } else {
                    cleanItem = enchantedItem.copy();
                    EnchantmentHelper.set(cleanItem, remaining);
                }
            } else {
                // regular items lose all non-curse enchantments
                cleanItem = enchantedItem.copy();
                cleanItem.remove(DataComponentTypes.REPAIR_COST);
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(cleanItem);
                ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchants);
                builder.remove(enchantment -> !enchantment.value().isCursed());
                EnchantmentHelper.set(cleanItem, builder.build());
            }

            // return the clean item to the player
            if (!player.getInventory().insertStack(cleanItem)) {
                player.dropItem(cleanItem, false);
            }
        }

        input.removeStack(bookSlot, 1);
        input.setStack(enchantedSlot, ItemStack.EMPTY);

        // skip vanilla take behavior without spawning experience
        ci.cancel();
    }
}
