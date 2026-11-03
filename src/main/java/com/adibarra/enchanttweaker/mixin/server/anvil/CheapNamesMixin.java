package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description renaming items in anvils costs 1 level
 * @environment server
 */
@Mixin(
    value = AnvilMenu.class)
public abstract class CheapNamesMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    @SuppressWarnings("unused")
    private CheapNamesMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory,
        ContainerLevelAccess context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(
        method = "createResult()V",
        at = @At("TAIL"))
    private void enchanttweaker$cheapNames$modifyLevelCost(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("CheapNamesMixin"))
            return;
        if (this.inputSlots.getItem(1).isEmpty() && !this.resultSlots.getItem(0).isEmpty()) {
            cost.set(1);
        }
    }
}
