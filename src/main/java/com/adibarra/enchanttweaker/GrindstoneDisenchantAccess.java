package com.adibarra.enchanttweaker;

import net.minecraft.world.Container;

/**
 * duck interface for communication between grindstone mixins avoids referencing
 * mixin class types in lvt
 */
public interface GrindstoneDisenchantAccess {
    int enchanttweaker$getBookSlot();

    Container enchanttweaker$getInput();
}
