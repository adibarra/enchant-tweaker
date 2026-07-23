package com.adibarra.enchanttweaker;

import net.minecraft.inventory.Inventory;

/**
 * duck interface for communication between grindstone mixins avoids referencing
 * mixin class types in lvt
 */
public interface GrindstoneDisenchantAccess {
    int enchanttweaker$getBookSlot();

    Inventory enchanttweaker$getInput();
}
