package com.adibarra.enchanttweaker.test;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

public class SmokeGameTest implements FabricGameTest {

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void smokeTestBlockPlace(TestContext helper) {
        BlockPos pos = new BlockPos(0, 2, 0);
        helper.setBlockState(pos, Blocks.STONE.getDefaultState());
        helper.expectBlock(Blocks.STONE, pos);
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void smokeTestBlockBreak(TestContext helper) {
        BlockPos pos = new BlockPos(0, 2, 0);
        helper.setBlockState(pos, Blocks.STONE.getDefaultState());
        helper.expectBlock(Blocks.STONE, pos);
        helper.setBlockState(pos, Blocks.AIR.getDefaultState());
        helper.dontExpectBlock(Blocks.STONE, pos);
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void smokeTestModLoaded(TestContext helper) {
        helper.assertTrue(ETMixinPlugin.getConfig() != null, "ETMixinPlugin config should be initialized");
        helper.complete();
    }
}
