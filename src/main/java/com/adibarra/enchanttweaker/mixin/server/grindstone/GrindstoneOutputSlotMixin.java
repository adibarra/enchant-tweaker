package com.adibarra.enchanttweaker.mixin.server.grindstone;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
    targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneOutputSlotMixin {

    @Shadow
    @Final
    GrindstoneMenu field_16780; // parent handler reference

    @Inject(
        method = "onTakeItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$grindstoneDisenchant$onTakeItem(Player player, ItemStack stack, CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("GrindstoneDisenchantMixin"))
            return;

        int bookSlot = ((GrindstoneDisenchantAccess) field_16780).enchanttweaker$getBookSlot();
        if (bookSlot < 0)
            return; // not a disenchant operation, let vanilla handle

        Container input = ((GrindstoneDisenchantAccess) field_16780).enchanttweaker$getInput();
        int enchantedSlot = bookSlot == 0 ? 1 : 0;
        ItemStack enchantedItem = input.getItem(enchantedSlot);
        boolean keepItem = ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant_keep_item", true);

        if (keepItem && !enchantedItem.isEmpty()) {
            // create a clean enchanted item
            ItemStack cleanItem;
            if (enchantedItem.is(Items.ENCHANTED_BOOK)) {
                // split books by removing the extracted enchantment
                ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(enchantedItem);
                Enchantment extracted = null;
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
                    if (!entry.getKey().value().isCurse()) {
                        extracted = entry.getKey().value();
                        break;
                    }
                }
                ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchants);
                if (extracted != null) {
                    Enchantment extractedEnchantment = extracted;
                    builder.removeIf(enchantment -> enchantment.value() == extractedEnchantment);
                }
                ItemEnchantments remaining = builder.toImmutable();
                if (remaining.isEmpty()) {
                    cleanItem = enchantedItem.transmuteCopy(Items.BOOK, enchantedItem.getCount());
                    cleanItem.remove(DataComponents.REPAIR_COST);
                    cleanItem.remove(DataComponents.STORED_ENCHANTMENTS);
                    cleanItem.remove(DataComponents.ENCHANTMENTS);
                } else {
                    cleanItem = enchantedItem.copy();
                    EnchantmentHelper.setEnchantments(cleanItem, remaining);
                }
            } else {
                // regular items lose all non-curse enchantments
                cleanItem = enchantedItem.copy();
                cleanItem.remove(DataComponents.REPAIR_COST);
                ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(cleanItem);
                ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchants);
                builder.removeIf(enchantment -> !enchantment.value().isCurse());
                EnchantmentHelper.setEnchantments(cleanItem, builder.toImmutable());
            }

            // return the clean item to the player
            if (!player.getInventory().add(cleanItem)) {
                player.drop(cleanItem, false);
            }
        }

        input.removeItem(bookSlot, 1);
        input.setItem(enchantedSlot, ItemStack.EMPTY);

        // skip vanilla take behavior without spawning experience
        ci.cancel();
    }
}
