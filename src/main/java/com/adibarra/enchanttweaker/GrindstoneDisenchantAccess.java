package com.adibarra.enchanttweaker;

import net.minecraft.inventory.Inventory;

/**
 * Duck interface for cross-mixin communication between GrindstoneDisenchantMixin
 * and GrindstoneOutputSlotMixin. Avoids referencing mixin class types in LVT.
 */
public interface GrindstoneDisenchantAccess {
    int enchanttweaker$getBookSlot();
    Inventory enchanttweaker$getInput();
}
