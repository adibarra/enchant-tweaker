package com.adibarra.enchanttweaker.mixin.server.tweak;

import java.util.List;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.MendingLevelAccess;
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
    protected abstract int xpToDurability(int i);

    @Shadow
    protected abstract int durabilityToXp(int i);

    @Shadow
    protected abstract int repairPlayerItems(Player player, int amount);

    @Inject(
        method = "repairPlayerItems(Lnet/minecraft/world/entity/player/Player;I)I",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$betterMending$modifyRepairPlayerGears(Player player, int amount,
        CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("BetterMendingMixin"))
            return;
        Inventory inv = player.getInventory();
        ItemStack repairItem = ADUtils.getMatchingItem(
            List.of(new ADUtils.Inventory(inv.getSelected()), new ADUtils.Inventory(inv.offhand),
                new ADUtils.Inventory(inv.armor), ADUtils.getPlayerHotbar(player), new ADUtils.Inventory(inv.items)),
            (stack) -> EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, stack) > 0 && stack.isDamaged());

        if (repairItem == null) {
            cir.setReturnValue(amount);
            return;
        }

        // use the running `amount` (xp still available on this call), not the orb's
        // original this.amount
        int i = Math.min(this.xpToDurability(amount), repairItem.getDamageValue());
        repairItem.setDamageValue(repairItem.getDamageValue() - i);
        // pass the repaired item's mending level to moremending
        if ((Object) this instanceof MendingLevelAccess access) {
            access.enchanttweaker$setMendingLevel(
                EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, repairItem));
        }
        int experience = amount - this.durabilityToXp(i);
        if (experience > 0) {
            cir.setReturnValue(this.repairPlayerItems(player, experience));
            return;
        }
        cir.setReturnValue(0);
    }
}
