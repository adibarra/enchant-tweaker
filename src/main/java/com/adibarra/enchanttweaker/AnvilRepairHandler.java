package com.adibarra.enchanttweaker;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

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

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        // gate callback before spectator handling
        if (hand != Hand.MAIN_HAND)
            return ActionResult.PASS;
        if (!player.isSneaking() || player.isSpectator())
            return ActionResult.PASS;
        // read the repair setting directly
        if (!ETMixinPlugin.getConfig().getOrDefault("anvil_repair", false))
            return ActionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(Blocks.DAMAGED_ANVIL) && !state.isOf(Blocks.CHIPPED_ANVIL))
            return ActionResult.PASS;
        if (!world.canPlayerModifyAt(player, pos))
            return ActionResult.PASS;

        ItemStack held = player.getMainHandStack();
        // a nonpositive cost disables repair
        int ingotCost = Math.max(0, ETMixinPlugin.getConfig().getOrDefault("anvil_repair_ingot_cost", 9));
        if (ingotCost <= 0)
            return ActionResult.PASS;

        // iron blocks work only for costs divisible by nine
        int consumeCount;
        if (held.isOf(Items.IRON_INGOT) && held.getCount() >= ingotCost) {
            consumeCount = ingotCost;
        } else if (held.isOf(Items.IRON_BLOCK) && ingotCost % 9 == 0 && held.getCount() >= ingotCost / 9) {
            consumeCount = ingotCost / 9;
        } else {
            return ActionResult.PASS;
        }

        // swing client-side, repair server-side
        if (world.isClient)
            return ActionResult.SUCCESS;

        BlockState repaired;
        if (state.isOf(Blocks.DAMAGED_ANVIL)) {
            repaired = Blocks.CHIPPED_ANVIL.getDefaultState().with(AnvilBlock.FACING, state.get(AnvilBlock.FACING));
        } else {
            repaired = Blocks.ANVIL.getDefaultState().with(AnvilBlock.FACING, state.get(AnvilBlock.FACING));
        }

        world.setBlockState(pos, repaired, Block.NOTIFY_ALL);
        world.syncWorldEvent(WorldEvents.ANVIL_USED, pos, 0);

        if (!player.isInCreativeMode()) {
            held.decrement(consumeCount);
        }

        return ActionResult.SUCCESS;
    }
}
