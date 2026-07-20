package com.adibarra.enchanttweaker;

/** duck interface for cross-mixin communication between MoreMendingMixin and BetterMendingMixin */
public interface MendingLevelAccess {
    void enchanttweaker$setMendingLevel(int level);
}
