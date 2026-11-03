package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description lets mending effect activate on any item in the inventory
 *              priority order: main-hand -> off-hand -> armor -> hotbar ->
 *              inventory
 * @environment server
 */
@Mixin(
    value = ExperienceOrb.class)
public abstract class BetterMendingMixin {

    @Shadow
    protected abstract int repairPlayerItems(ServerPlayer player, int amount);

    @Inject(
        method = "repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$betterMending$modifyRepairPlayerGears(ServerPlayer player, int amount,
        CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("BetterMendingMixin"))
            return;
        Inventory inv = player.getInventory();
        ItemStack repairItem = ADUtils.getMatchingItem(
            List.of(new ADUtils.Inventory(player.getMainHandItem()), new ADUtils.Inventory(player.getOffhandItem()),
                new ADUtils.Inventory(
                    List.of(player.getItemBySlot(EquipmentSlot.FEET), player.getItemBySlot(EquipmentSlot.LEGS),
                        player.getItemBySlot(EquipmentSlot.CHEST), player.getItemBySlot(EquipmentSlot.HEAD))),
                ADUtils.getPlayerHotbar(player), new ADUtils.Inventory(inv.getNonEquipmentItems())),
            (stack) -> ADUtils.getEnchantmentLevel(stack, Enchantments.MENDING) > 0 && stack.isDamaged());

        if (repairItem == null) {
            cir.setReturnValue(amount);
            return;
        }

        // use the running `amount` (xp still available on this call), not the orb's
        // original this.amount
        int durability = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), repairItem, amount);
        if (durability <= 0) {
            cir.setReturnValue(amount);
            return;
        }
        int repaired = Math.min(durability, repairItem.getDamageValue());
        repairItem.setDamageValue(repairItem.getDamageValue() - repaired);
        int experience = amount - repaired * amount / durability;
        if (experience > 0) {
            cir.setReturnValue(this.repairPlayerItems(player, experience));
            return;
        }
        cir.setReturnValue(0);
    }
}
