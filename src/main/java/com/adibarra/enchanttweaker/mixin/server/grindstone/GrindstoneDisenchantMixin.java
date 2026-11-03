package com.adibarra.enchanttweaker.mixin.server.grindstone;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.GrindstoneDisenchantAccess;

/**
 * @description extract enchantments into books using a grindstone
 * @environment server
 */
@Mixin(
    value = GrindstoneMenu.class)
public abstract class GrindstoneDisenchantMixin implements GrindstoneDisenchantAccess {

    @Shadow
    @Final
    private Container resultSlots;

    @Shadow
    @Final
    Container repairSlots;

    /**
     * input slot holding the book, or -1 when not disenchanting
     */
    @Unique
    private int enchanttweaker$bookSlot = -1;

    @Inject(
        method = "createResult()V",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$grindstoneDisenchant$updateResult(CallbackInfo ci) {
        enchanttweaker$bookSlot = -1;
        resultSlots.setItem(0, ItemStack.EMPTY);
        if (!ETMixinPlugin.getMixinConfig("GrindstoneDisenchantMixin")) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        ItemStack stack0 = repairSlots.getItem(0);
        ItemStack stack1 = repairSlots.getItem(1);

        // reject stacked inputs because grindstones process one item
        if (stack0.getCount() > 1 || stack1.getCount() > 1) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        int bookSlot = -1;
        int enchantedSlot = -1;
        if (stack0.is(Items.BOOK) && !stack1.isEmpty() && EnchantmentHelper.hasAnyEnchantments(stack1)) {
            bookSlot = 0;
            enchantedSlot = 1;
        } else if (stack1.is(Items.BOOK) && !stack0.isEmpty() && EnchantmentHelper.hasAnyEnchantments(stack0)) {
            bookSlot = 1;
            enchantedSlot = 0;
        }

        if (bookSlot == -1) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        enchanttweaker$bookSlot = bookSlot;
        ItemStack enchantedItem = repairSlots.getItem(enchantedSlot);
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(enchantedItem);

        List<Object2IntMap.Entry<Holder<Enchantment>>> nonCurse = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            if (!entry.getKey().is(EnchantmentTags.CURSE)) {
                nonCurse.add(entry);
            }
        }

        if (nonCurse.isEmpty()) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        ItemStack outputBook = new ItemStack(Items.ENCHANTED_BOOK);

        if (enchantedItem.is(Items.ENCHANTED_BOOK) && nonCurse.size() > 1) {
            Object2IntMap.Entry<Holder<Enchantment>> picked = nonCurse.getFirst();
            outputBook.enchant(picked.getKey(), picked.getIntValue());
        } else {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : nonCurse) {
                outputBook.enchant(entry.getKey(), entry.getIntValue());
            }
        }

        resultSlots.setItem(0, outputBook);
        ((GrindstoneMenu) (Object) this).broadcastChanges();
        ci.cancel();
    }

    /**
     * returns the book input slot, or -1 when not disenchanting
     */
    @Unique
    public int enchanttweaker$getBookSlot() {
        return enchanttweaker$bookSlot;
    }

    /** exposes the input inventory for the output slot mixin */
    @Unique
    public Container enchanttweaker$getInput() {
        return repairSlots;
    }
}
