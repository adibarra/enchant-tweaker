package com.adibarra.enchanttweaker.test;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

public class AnvilGameTest implements FabricGameTest {

    // ─── CheapNames ──────────────────────────────────────────────────
    // CheapNamesMixin overrides levelCost to 1 at TAIL of updateResult when slot 1 is empty.
    // A sword with REPAIR_COST=10 normally costs 11 levels to rename; mixin forces it to 1.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cheapNamesEnabled(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponentTypes.REPAIR_COST, 10);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        ETTestHelper.setAnvilNewName(handler, "Test Sword");
        handler.updateResult();
        try {
            helper.assertTrue(handler.getLevelCost() == 1,
                "CheapNames should force rename cost to 1 (got " + handler.getLevelCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cheapNamesDisabled(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponentTypes.REPAIR_COST, 10);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        ETTestHelper.setAnvilNewName(handler, "Test Sword");
        handler.updateResult();
        helper.assertTrue(handler.getLevelCost() > 1,
            "Vanilla rename with REPAIR_COST=10 should cost > 1 (got " + handler.getLevelCost() + ")");
        helper.complete();
    }

    // ─── NotTooExpensive ─────────────────────────────────────────────
    // NotTooExpensiveMixin replaces the vanilla 40-level cap with nte_max_cost (default MAX_INT).
    // A sword with REPAIR_COST=50 produces a total cost > 40 → vanilla blocks it; mixin allows it.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void notTooExpensiveEnabled(TestContext helper) {
        ETTestHelper.setFeature("not_too_expensive", true);
        // nte_max_cost defaults to Integer.MAX_VALUE — any realistic cost passes
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(100);
        sword.set(DataComponentTypes.REPAIR_COST, 50);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        handler.updateResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "NotTooExpensive should allow repair beyond the 40-level cap (result was empty)");
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void notTooExpensiveDisabled(TestContext helper) {
        ETTestHelper.setFeature("not_too_expensive", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(100);
        sword.set(DataComponentTypes.REPAIR_COST, 50);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        handler.updateResult();
        helper.assertTrue(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
            "Vanilla 40-level cap should block repair when not_too_expensive is disabled");
        helper.complete();
    }

    // ─── SturdyAnvils ────────────────────────────────────────────────
    // SturdyAnvilsMixin modifies the 0.12f damage chance in method_24922 (onTakeOutput lambda).
    // Requires a real ScreenHandlerContext so context.run() invokes the lambda.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsNeverBreaks(TestContext helper) {
        ETTestHelper.setFeature("sturdy_anvils", true);
        ETMixinPlugin.getConfig().set("anvil_damage_chance", "0.0");
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
        ServerWorld world = helper.getWorld();
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        // Survival player so the damage lambda body actually runs (creative mode skips it)
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        player.addExperienceLevels(10000);
        try {
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            for (int i = 0; i < 10; i++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.setDamage(100);
                ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
                handler.updateResult();
                ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
                if (!result.isEmpty()) {
                    ETTestHelper.invokeOnTakeOutput(handler, player, result);
                }
            }
            helper.expectBlock(Blocks.ANVIL, anvilPos);
        } finally {
            ETTestHelper.setFeature("sturdy_anvils", false);
            ETMixinPlugin.getConfig().set("anvil_damage_chance", "0.06");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsDisabled(TestContext helper) {
        // Vanilla: 12% per use. P(no damage in 500 uses) = 0.88^500 ≈ 3e-30 → virtually certain.
        ETTestHelper.setFeature("sturdy_anvils", false);
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        // Survival player so the damage lambda body actually runs (creative mode skips it)
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        player.addExperienceLevels(10000);
        boolean damagedOnce = false;
        for (int i = 0; i < 500; i++) {
            helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.updateResult();
            ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            if (!result.isEmpty()) {
                ETTestHelper.invokeOnTakeOutput(handler, player, result);
                if (!world.getBlockState(absPos).isOf(Blocks.ANVIL)) {
                    damagedOnce = true;
                    break;
                }
            }
        }
        helper.assertTrue(damagedOnce,
            "Vanilla anvil should take damage at 12% per use (zero damage in 500 attempts)");
        helper.complete();
    }

    // ─── PriorWorkFree ───────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeEnabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_free", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // Sword with no prior work (REPAIR_COST = 0)
        ItemStack swordNoPenalty = new ItemStack(Items.DIAMOND_SWORD);
        swordNoPenalty.setDamage(50);
        // Same sword but with a prior work penalty
        ItemStack swordWithPenalty = swordNoPenalty.copy();
        swordWithPenalty.set(DataComponentTypes.REPAIR_COST, 4);
        // Compute base cost with no prior work
        AnvilScreenHandler base = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(base, swordNoPenalty, new ItemStack(Items.DIAMOND));
        base.updateResult();
        int baseCost = base.getLevelCost();
        // With feature enabled, prior work penalty should be removed
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, swordWithPenalty, new ItemStack(Items.DIAMOND));
        handler.updateResult();
        int penaltyCost = handler.getLevelCost();
        try {
            helper.assertTrue(penaltyCost == baseCost,
                "Prior work cost should be removed when enabled (base=" + baseCost + " got=" + penaltyCost + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeDisabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_free", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // Sword with no prior work (REPAIR_COST = 0)
        ItemStack swordNoPenalty = new ItemStack(Items.DIAMOND_SWORD);
        swordNoPenalty.setDamage(50);
        // Same sword but with a prior work penalty
        ItemStack swordWithPenalty = swordNoPenalty.copy();
        swordWithPenalty.set(DataComponentTypes.REPAIR_COST, 4);
        // Compute base cost with no prior work
        AnvilScreenHandler base = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(base, swordNoPenalty, new ItemStack(Items.DIAMOND));
        base.updateResult();
        int baseCost = base.getLevelCost();
        // With feature disabled, prior work penalty should be included
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, swordWithPenalty, new ItemStack(Items.DIAMOND));
        handler.updateResult();
        int penaltyCost = handler.getLevelCost();
        helper.assertTrue(penaltyCost > baseCost,
            "Prior work cost should be included when disabled (base=" + baseCost + " got=" + penaltyCost + ")");
        helper.complete();
    }

    // ─── PriorWorkCheaper ────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperEnabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETMixinPlugin.getConfig().set("pw_cost_multiplier", "1.0");
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // formula: round(1.0 * 4 + 1) = 5
            helper.assertTrue(result == 5,
                "getNextCost(4) with multiplier 1.0 should be 5 (got " + result + ")");
        } finally {
            ETMixinPlugin.getConfig().set("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperDisabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", false);
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // vanilla: 2*4+1 = 9
            helper.assertTrue(result == 9,
                "getNextCost(4) should be vanilla 9 when disabled (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", true);
        }
        helper.complete();
    }
}
