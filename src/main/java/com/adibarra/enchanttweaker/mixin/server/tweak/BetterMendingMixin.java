package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.List;

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

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.MendingLevelAccess;
import com.adibarra.utils.ADUtils;

/**
 * @description lets Mending effect activate on any item in the inventory
 *              priority order: Main-Hand -> Off-Hand -> Armor -> Hotbar ->
 *              Inventory
 * @environment Server
 */
@Mixin(
    value = ExperienceOrbEntity.class)
public abstract class BetterMendingMixin {

    @Shadow
    protected abstract int getMendingRepairAmount(int i);

    @Shadow
    protected abstract int getMendingRepairCost(int i);

    @Shadow
    protected abstract int repairPlayerGears(PlayerEntity player, int amount);

    @Inject(
        method = "repairPlayerGears(Lnet/minecraft/entity/player/PlayerEntity;I)I",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$betterMending$modifyRepairPlayerGears(PlayerEntity player, int amount,
        CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("BetterMendingMixin"))
            return;
        PlayerInventory inv = player.getInventory();
        ItemStack repairItem = ADUtils.getMatchingItem(
            List.of(new ADUtils.Inventory(inv.getMainHandStack()), new ADUtils.Inventory(inv.offHand),
                new ADUtils.Inventory(inv.armor), ADUtils.getPlayerHotbar(player), new ADUtils.Inventory(inv.main)),
            (stack) -> EnchantmentHelper.getLevel(Enchantments.MENDING, stack) > 0 && stack.isDamaged());

        if (repairItem == null) {
            cir.setReturnValue(amount);
            return;
        }

        // use the running `amount` (XP still available on THIS call), not the orb's
        // original this.amount
        int i = Math.min(this.getMendingRepairAmount(amount), repairItem.getDamage());
        repairItem.setDamage(repairItem.getDamage() - i);
        // pass the repaired item's mending level to MoreMending
        if ((Object) this instanceof MendingLevelAccess access) {
            access.enchanttweaker$setMendingLevel(EnchantmentHelper.getLevel(Enchantments.MENDING, repairItem));
        }
        int experience = amount - this.getMendingRepairCost(i);
        if (experience > 0) {
            cir.setReturnValue(this.repairPlayerGears(player, experience));
            return;
        }
        cir.setReturnValue(0);
    }
}
