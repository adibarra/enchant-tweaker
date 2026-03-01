package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allows repairing damaged anvils by sneak+right-clicking with iron.
 * @environment Server
 */
@Mixin(value=AnvilBlock.class)
public abstract class AnvilRepairMixin {

    @Inject(
        method="onUse(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$anvilRepair$repairWithIron(
            BlockState state, World world, BlockPos pos, PlayerEntity player,
            BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!ETMixinPlugin.getMixinConfig("AnvilRepairMixin")) return;
        if (!player.isSneaking()) return;
        if (world.isClient) { cir.setReturnValue(ActionResult.SUCCESS); return; }

        if (state.isOf(Blocks.ANVIL)) return;

        ItemStack held = player.getMainHandStack();
        int ingotCost = ETMixinPlugin.getConfig().getOrDefault("anvil_repair_ingot_cost", 9);
        int blockCost = ETMixinPlugin.getConfig().getOrDefault("anvil_repair_block_cost", 1);

        boolean isIngot = ingotCost > 0 && held.isOf(Items.IRON_INGOT) && held.getCount() >= ingotCost;
        boolean isBlock = blockCost > 0 && held.isOf(Items.IRON_BLOCK) && held.getCount() >= blockCost;
        if (!isIngot && !isBlock) return;

        BlockState repaired;
        if (state.isOf(Blocks.DAMAGED_ANVIL)) {
            repaired = Blocks.CHIPPED_ANVIL.getDefaultState()
                .with(AnvilBlock.FACING, state.get(AnvilBlock.FACING));
        } else if (state.isOf(Blocks.CHIPPED_ANVIL)) {
            repaired = Blocks.ANVIL.getDefaultState()
                .with(AnvilBlock.FACING, state.get(AnvilBlock.FACING));
        } else {
            return;
        }

        world.setBlockState(pos, repaired, Block.NOTIFY_ALL);
        world.syncWorldEvent(1030, pos, 0);

        if (!player.isInCreativeMode()) {
            held.decrement(isIngot ? ingotCost : blockCost);
        }

        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
