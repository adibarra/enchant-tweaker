package com.adibarra.enchanttweaker.mixin.server.enhanced;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description scales binding curse to avoid dropping items on death
 * @environment server
 */
@Mixin(
    value = Inventory.class)
public abstract class MoreBindingMixin {

    // stores kept armor for respawn restoration on the after_respawn event
    @Unique
    private static final Map<UUID, Map<Integer, ItemStack>> BOUND_ARMOR = new ConcurrentHashMap<>();

    static {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Map<Integer, ItemStack> armorMap = BOUND_ARMOR.remove(oldPlayer.getUUID());
            if (armorMap == null)
                return;
            for (Map.Entry<Integer, ItemStack> entry : armorMap.entrySet()) {
                EquipmentSlot slot = EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, entry.getKey());
                newPlayer.setItemSlot(slot, entry.getValue());
            }
            newPlayer.getInventory().setChanged();
            newPlayer.containerMenu.broadcastChanges();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BOUND_ARMOR.remove(handler.getPlayer().getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BOUND_ARMOR.clear());
    }

    @Shadow
    @Final
    public NonNullList<ItemStack> armor;

    @Shadow
    @Final
    public Player player;

    @ModifyExpressionValue(
        method = "dropAll()V",
        at = @At(
            ordinal = 0,
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean enchanttweaker$moreBinding$modifyDropAll(boolean orig, @Local ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("MoreBindingMixin"))
            return orig;
        if (orig)
            return true;
        if (!armor.contains(stack))
            return false;

        int bindingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BINDING_CURSE, stack);
        double step = ETMixinPlugin.getConfig().getOrDefault("more_binding_step", 0.1);
        // only server players participate in AFTER_RESPAWN, which consumes the stash
        if (ADUtils.bindingKeepsItem(bindingLevel, step, ThreadLocalRandom.current().nextFloat())) {
            if (player instanceof ServerPlayer)
                BOUND_ARMOR.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(armor.indexOf(stack),
                    stack.copy());
            return true;
        }
        return false;
    }
}
