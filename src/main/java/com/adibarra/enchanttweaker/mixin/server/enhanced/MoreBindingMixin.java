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
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
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
    value = PlayerInventory.class)
public abstract class MoreBindingMixin {

    // stores kept armor for respawn restoration on the after_respawn event
    @Unique
    private static final Map<UUID, Map<Integer, ItemStack>> BOUND_ARMOR = new ConcurrentHashMap<>();

    static {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Map<Integer, ItemStack> armorMap = BOUND_ARMOR.remove(oldPlayer.getUuid());
            if (armorMap == null)
                return;
            for (Map.Entry<Integer, ItemStack> entry : armorMap.entrySet()) {
                EquipmentSlot slot = EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, entry.getKey());
                newPlayer.equipStack(slot, entry.getValue());
            }
            newPlayer.getInventory().markDirty();
            newPlayer.currentScreenHandler.sendContentUpdates();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BOUND_ARMOR.remove(handler.getPlayer().getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BOUND_ARMOR.clear());
    }

    @Shadow
    @Final
    public DefaultedList<ItemStack> armor;

    @Shadow
    @Final
    public PlayerEntity player;

    @ModifyExpressionValue(
        method = "dropAll()V",
        at = @At(
            ordinal = 0,
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private boolean enchanttweaker$moreBinding$modifyDropAll(boolean orig, @Local ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("MoreBindingMixin"))
            return orig;
        if (orig)
            return true;
        if (!armor.contains(stack))
            return false;

        int bindingLevel = EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, stack);
        double step = ETMixinPlugin.getConfig().getOrDefault("more_binding_step", 0.1);
        // only server players participate in AFTER_RESPAWN, which consumes the stash
        if (ADUtils.bindingKeepsItem(bindingLevel, step, ThreadLocalRandom.current().nextFloat())) {
            if (player instanceof ServerPlayerEntity)
                BOUND_ARMOR.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>()).put(armor.indexOf(stack),
                    stack.copy());
            return true;
        }
        return false;
    }
}
