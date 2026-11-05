package com.adibarra.enchanttweaker.mixin.server.enhanced;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description scales binding curse to avoid dropping items on death
 * @environment server
 */
@Mixin(
    value = EntityEquipment.class)
public abstract class MoreBindingMixin {

    // stores kept armor for respawn restoration on the after_respawn event
    @Unique
    private static final Map<UUID, Map<EquipmentSlot, ItemStack>> BOUND_ARMOR = new ConcurrentHashMap<>();

    static {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Map<EquipmentSlot, ItemStack> armorMap = BOUND_ARMOR.remove(oldPlayer.getUUID());
            if (armorMap == null)
                return;
            for (Map.Entry<EquipmentSlot, ItemStack> entry : armorMap.entrySet()) {
                newPlayer.setItemSlot(entry.getKey(), entry.getValue());
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
    private EnumMap<EquipmentSlot, ItemStack> items;

    @Unique
    private static boolean enchanttweaker$moreBinding$isKeptArmor(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.FEET && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.CHEST
            && slot != EquipmentSlot.HEAD)
            return false;
        if (stack.isEmpty())
            return false;
        int bindingLevel = ADUtils.getEnchantmentLevel(stack, Enchantments.BINDING_CURSE);
        if (bindingLevel <= 0)
            return false;
        double step = ETMixinPlugin.getConfig().getOrDefault("more_binding_step", 0.1);
        return ADUtils.bindingKeepsItem(bindingLevel, step, ThreadLocalRandom.current().nextFloat());
    }

    @Inject(
        method = "dropAll(Lnet/minecraft/world/entity/LivingEntity;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$moreBinding$keepBoundArmor(LivingEntity owner, CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("MoreBindingMixin"))
            return;
        ci.cancel();
        for (Map.Entry<EquipmentSlot, ItemStack> entry : new EnumMap<>(items).entrySet()) {
            EquipmentSlot slot = entry.getKey();
            ItemStack stack = entry.getValue();
            if (enchanttweaker$moreBinding$isKeptArmor(slot, stack)) {
                // only server players participate in AFTER_RESPAWN, which consumes the stash
                if (owner instanceof ServerPlayer)
                    BOUND_ARMOR.computeIfAbsent(owner.getUUID(), k -> new EnumMap<>(EquipmentSlot.class)).put(slot,
                        stack.copy());
                continue;
            }
            if (!stack.isEmpty()) {
                owner.drop(stack, true, false);
                items.put(slot, ItemStack.EMPTY);
            }
        }
    }
}
