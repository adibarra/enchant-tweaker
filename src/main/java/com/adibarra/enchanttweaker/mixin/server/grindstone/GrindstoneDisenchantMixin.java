package com.adibarra.enchanttweaker.mixin.server.grindstone;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GrindstoneScreenHandler;
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
 * @description allow extracting enchantments from items into books via
 *              grindstone place an enchanted item + regular book in the
 *              grindstone to extract enchantments also supports splitting
 *              multi-enchantment books
 * @environment Server
 */
@Mixin(
    value = GrindstoneScreenHandler.class)
public abstract class GrindstoneDisenchantMixin implements GrindstoneDisenchantAccess {

    @Shadow
    @Final
    private Inventory result;

    @Shadow
    @Final
    Inventory input;

    /**
     * which input slot holds the book (0 or 1), or -1 if not a disenchant operation
     */
    @Unique
    private int enchanttweaker$bookSlot = -1;

    @Inject(
        method = "updateResult()V",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$grindstoneDisenchant$updateResult(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("GrindstoneDisenchantMixin")) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        ItemStack stack0 = input.getStack(0);
        ItemStack stack1 = input.getStack(1);

        // reject stacked inputs because grindstones process one item at a time
        if (stack0.getCount() > 1 || stack1.getCount() > 1) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        int bookSlot = -1;
        int enchantedSlot = -1;
        if (stack0.isOf(Items.BOOK) && !stack1.isEmpty() && EnchantmentHelper.hasEnchantments(stack1)) {
            bookSlot = 0;
            enchantedSlot = 1;
        } else if (stack1.isOf(Items.BOOK) && !stack0.isEmpty() && EnchantmentHelper.hasEnchantments(stack0)) {
            bookSlot = 1;
            enchantedSlot = 0;
        }

        if (bookSlot == -1) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        enchanttweaker$bookSlot = bookSlot;
        ItemStack enchantedItem = input.getStack(enchantedSlot);
        ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(enchantedItem);

        List<Object2IntMap.Entry<RegistryEntry<Enchantment>>> nonCurse = new ArrayList<>();
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentsMap()) {
            if (!entry.getKey().value().isCursed()) {
                nonCurse.add(entry);
            }
        }

        if (nonCurse.isEmpty()) {
            enchanttweaker$bookSlot = -1;
            return;
        }

        ItemStack outputBook = new ItemStack(Items.ENCHANTED_BOOK);

        if (enchantedItem.isOf(Items.ENCHANTED_BOOK) && nonCurse.size() > 1) {
            Object2IntMap.Entry<RegistryEntry<Enchantment>> picked = nonCurse.getFirst();
            outputBook.addEnchantment(picked.getKey().value(), picked.getIntValue());
        } else {
            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : nonCurse) {
                outputBook.addEnchantment(entry.getKey().value(), entry.getIntValue());
            }
        }

        result.setStack(0, outputBook);
        ((GrindstoneScreenHandler) (Object) this).sendContentUpdates();
        ci.cancel();
    }

    /**
     * gets which input slot holds the book (0 or 1), or -1 if not a disenchant
     * operation
     */
    @Unique
    public int enchanttweaker$getBookSlot() {
        return enchanttweaker$bookSlot;
    }

    /** exposes the input inventory for the output slot mixin */
    @Unique
    public Inventory enchanttweaker$getInput() {
        return input;
    }
}
