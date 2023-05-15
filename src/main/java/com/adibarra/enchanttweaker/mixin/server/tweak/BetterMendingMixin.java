package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.utils.ADUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * @description Lets Mending effect activate on any item in the inventory.
 * Priority order: Main-Hand -> Off-Hand -> Armor -> Hotbar -> Inventory.
 * @environment Server
 */
@Mixin(value=ExperienceOrbEntity.class, priority=1543)
public abstract class BetterMendingMixin {

    @Shadow
    private int amount;

    @Shadow
    protected abstract int getMendingRepairAmount(int i);

    @Shadow
    protected abstract int getMendingRepairCost(int i);

    @Shadow
    protected abstract int repairPlayerGears(PlayerEntity player, int amount);

    @Inject(
        method="repairPlayerGears(Lnet/minecraft/entity/player/PlayerEntity;I)I",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$betterMending$modifyRepairPlayerGears(PlayerEntity player, int amount, CallbackInfoReturnable<Integer> cir) {
        PlayerInventory inv = player.getInventory();
        ItemStack repairItem = ADUtils.getMatchingItem(
            List.of(
                new ADUtils.Inventory(inv.getMainHandStack()),
                new ADUtils.Inventory(inv.offHand),
                new ADUtils.Inventory(inv.armor),
                ADUtils.getPlayerHotbar(player),
                new ADUtils.Inventory(inv.main)
            ),
            (stack) -> EnchantmentHelper.getLevel(Enchantments.MENDING, stack) > 0 && stack.isDamaged()
        );

        if (repairItem == null) {
            cir.setReturnValue(amount);
            return;
        }

        int i = Math.min(this.getMendingRepairAmount(this.amount), repairItem.getDamage());
        repairItem.setDamage(repairItem.getDamage() - i);
        int experience = amount - this.getMendingRepairCost(i);
        if (experience > 0) {
            cir.setReturnValue(this.repairPlayerGears(player, experience));
            return;
        }
        cir.setReturnValue(0);
    }
}
