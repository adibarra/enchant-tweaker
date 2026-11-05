package com.adibarra.enchanttweaker;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** repairs damaged anvils with iron while sneaking */
public final class AnvilRepairHandler {

    // tracks callback registration for diagnostics
    private static volatile boolean registered = false;

    private AnvilRepairHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(AnvilRepairHandler::onUseBlock);
        registered = true;
    }

    public static boolean isRegistered() {
        return registered;
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        // gate callback before spectator handling
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        if (!player.isShiftKeyDown() || player.isSpectator())
            return InteractionResult.PASS;
        // read the repair setting directly
        if (!ETMixinPlugin.getConfig().getOrDefault("anvil_repair", false))
            return InteractionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!state.is(Blocks.DAMAGED_ANVIL) && !state.is(Blocks.CHIPPED_ANVIL))
            return InteractionResult.PASS;
        if (!world.mayInteract(player, pos))
            return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        // a nonpositive cost disables repair
        int ingotCost = Math.max(0, ETMixinPlugin.getConfig().getOrDefault("anvil_repair_ingot_cost", 9));
        if (ingotCost <= 0)
            return InteractionResult.PASS;

        // iron blocks work only for costs divisible by nine
        int consumeCount;
        if (held.is(Items.IRON_INGOT) && held.getCount() >= ingotCost) {
            consumeCount = ingotCost;
        } else if (held.is(Items.IRON_BLOCK) && ingotCost % 9 == 0 && held.getCount() >= ingotCost / 9) {
            consumeCount = ingotCost / 9;
        } else {
            return InteractionResult.PASS;
        }

        // swing client-side, repair server-side
        if (world.isClientSide())
            return InteractionResult.SUCCESS;

        BlockState repaired;
        if (state.is(Blocks.DAMAGED_ANVIL)) {
            repaired = Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING,
                state.getValue(AnvilBlock.FACING));
        } else {
            repaired = Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, state.getValue(AnvilBlock.FACING));
        }

        world.setBlock(pos, repaired, Block.UPDATE_ALL);
        if (world instanceof ServerLevel serverLevel)
            serverLevel.levelEvent(null, LevelEvent.SOUND_ANVIL_USED, pos, 0);

        if (!player.hasInfiniteMaterials()) {
            held.shrink(consumeCount);
        }

        return InteractionResult.SUCCESS;
    }
}
