package com.adibarra.enchanttweaker.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import com.adibarra.enchanttweaker.ETMixinPlugin;

public class SmokeGameTest implements FabricGameTest {

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void smokeTestBlockPlace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 2, 0);
        helper.setBlock(pos, Blocks.STONE.defaultBlockState());
        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void smokeTestBlockBreak(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 2, 0);
        helper.setBlock(pos, Blocks.STONE.defaultBlockState());
        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.setBlock(pos, Blocks.AIR.defaultBlockState());
        helper.assertBlockNotPresent(Blocks.STONE, pos);
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void smokeTestModLoaded(GameTestHelper helper) {
        helper.assertTrue(ETMixinPlugin.getConfig() != null, "ETMixinPlugin config should be initialized");
        helper.succeed();
    }
}
