package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Method;
import java.util.Map;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import com.adibarra.enchanttweaker.ETMixinPlugin;

public class AnvilGameTest implements FabricGameTest {

    // cheap names applies to single-input renames
    // repair cost ten normally raises rename cost

    @GameTest(
        templateName = EMPTY_STRUCTURE)
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
            helper.assertFalse(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "CheapNames rename must produce an output");
            helper.assertTrue(handler.getLevelCost() == 1,
                "CheapNames should force rename cost to 1 (got " + handler.getLevelCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesDisabled(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponentTypes.REPAIR_COST, 10);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        ETTestHelper.setAnvilNewName(handler, "Test Sword");
        handler.updateResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "Vanilla rename must produce an output");
            helper.assertTrue(handler.getLevelCost() > 1,
                "Vanilla rename with REPAIR_COST=10 should cost > 1 (got " + handler.getLevelCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesEmptyOperationStaysEmpty(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponentTypes.REPAIR_COST, 10);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        handler.updateResult();
        try {
            helper.assertTrue(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "CheapNames must not create a result when no rename or second input was supplied");
            helper.assertTrue(handler.getLevelCost() == 10,
                "CheapNames must not rewrite the vanilla cost of an empty operation (got " + handler.getLevelCost()
                    + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    // not too expensive raises the vanilla cost cap
    // the configured maximum defaults to integer max
    // repair cost fifty exceeds forty levels

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void notTooExpensiveEnabled(TestContext helper) {
        ETTestHelper.setFeature("not_too_expensive", true);
        ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
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

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void notTooExpensiveDisabled(TestContext helper) {
        ETTestHelper.setFeature("not_too_expensive", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(100);
        sword.set(DataComponentTypes.REPAIR_COST, 50);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        handler.updateResult();
        try {
            helper.assertTrue(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "Vanilla 40-level cap should block repair when not_too_expensive is disabled");
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
        }
        helper.complete();
    }

    // sturdy anvils controls damage after taking output
    // the handler needs a real screen context
    // the context runs the take-output lambda

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsNeverBreaks(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "0.0");
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
            ServerWorld world = helper.getWorld();
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            player.addExperienceLevels(10000);
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            for (int i = 0; i < 10; i++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.setDamage(100);
                ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
                handler.updateResult();
                ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
                helper.assertFalse(result.isEmpty(),
                    "A sturdy-anvil repair setup must produce an output before onTakeOutput");
                ETTestHelper.invokeOnTakeOutput(handler, player, result);
            }
            helper.expectBlock(Blocks.ANVIL, anvilPos);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsDisabled(TestContext helper) {
        // vanilla damage chance is twelve percent per use
        // a seeded random roll deterministically exercises the damage branch
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils");
        try {
            ETTestHelper.setFeature("sturdy_anvils", false);
            ServerWorld world = helper.getWorld();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            player.addExperienceLevels(10000);
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.updateResult();
            ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            helper.assertFalse(result.isEmpty(),
                "A vanilla-anvil repair setup must produce an output before onTakeOutput");

            long damageSeed = 0L;
            while (damageSeed < 1_000_000L
                && net.minecraft.util.math.random.Random.create(damageSeed).nextFloat() >= 0.12F)
                damageSeed++;
            helper.assertTrue(damageSeed < 1_000_000L,
                "a deterministic seed should be available for the vanilla 12% damage branch");
            player.getRandom().setSeed(damageSeed);
            float sampledChance = player.getRandom().nextFloat();
            helper.assertTrue(sampledChance < 0.12F,
                "the selected deterministic seed must exercise the vanilla damage branch (sample=" + sampledChance
                    + ")");
            player.getRandom().setSeed(damageSeed);
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.CHIPPED_ANVIL),
                "vanilla anvil use with a deterministic damage roll should produce a chipped anvil");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // prior work free

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeEnabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_free", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // sword without prior work
        ItemStack swordNoPenalty = new ItemStack(Items.DIAMOND_SWORD);
        swordNoPenalty.setDamage(50);
        // matching sword with prior work
        ItemStack swordWithPenalty = swordNoPenalty.copy();
        swordWithPenalty.set(DataComponentTypes.REPAIR_COST, 4);
        // calculate base cost without prior work
        AnvilScreenHandler base = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(base, swordNoPenalty, new ItemStack(Items.DIAMOND));
        base.updateResult();
        int baseCost = base.getLevelCost();
        // enabled prior work free removes the penalty
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

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeDisabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_free", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // sword without prior work
        ItemStack swordNoPenalty = new ItemStack(Items.DIAMOND_SWORD);
        swordNoPenalty.setDamage(50);
        // matching sword with prior work
        ItemStack swordWithPenalty = swordNoPenalty.copy();
        swordWithPenalty.set(DataComponentTypes.REPAIR_COST, 4);
        // calculate base cost without prior work
        AnvilScreenHandler base = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(base, swordNoPenalty, new ItemStack(Items.DIAMOND));
        base.updateResult();
        int baseCost = base.getLevelCost();
        // disabled prior work free retains the penalty
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, swordWithPenalty, new ItemStack(Items.DIAMOND));
        handler.updateResult();
        int penaltyCost = handler.getLevelCost();
        try {
            helper.assertTrue(penaltyCost > baseCost,
                "Prior work cost should be included when disabled (base=" + baseCost + " got=" + penaltyCost + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    // prior work cheaper

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperEnabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "1.0");
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // multiplier one yields five
            helper.assertTrue(result == 5, "getNextCost(4) with multiplier 1.0 should be 5 (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperDisabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", false);
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // vanilla multiplier yields nine
            helper.assertTrue(result == 9, "getNextCost(4) should be vanilla 9 when disabled (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperZeroMultiplier(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "0.0");
        ETMixinPlugin.clearCaches();
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // zero multiplier yields one
            helper.assertTrue(result == 1, "getNextCost(4) with multiplier 0.0 should be 1 (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperHighMultiplier(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "3.0");
        ETMixinPlugin.clearCaches();
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // multiplier three yields thirteen
            helper.assertTrue(result == 13, "getNextCost(4) with multiplier 3.0 should be 13 (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperNegativeMultiplier(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "-1.0");
        ETMixinPlugin.clearCaches();
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // negative multipliers clamp to zero
            // rounding then returns one
            helper.assertTrue(result == 1,
                "getNextCost(4) with multiplier -1.0 should clamp to 1 (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    // not too expensive custom maximum cost

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void notTooExpensiveCustomMaxCost(TestContext helper) {
        ETTestHelper.setFeature("not_too_expensive", true);
        ETTestHelper.setConfigValue("nte_max_cost", "50");
        ETMixinPlugin.clearCaches();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // repair cost sixty exceeds the configured cap
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(100);
        sword.set(DataComponentTypes.REPAIR_COST, 60);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        handler.updateResult();
        try {
            helper.assertTrue(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "Custom nte_max_cost=50 should block repair with cost > 50");
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    // sturdy anvils can always break

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsAlwaysBreaks(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "1.0");
            ServerWorld world = helper.getWorld();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            player.addExperienceLevels(10000);
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.updateResult();
            ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            helper.assertFalse(result.isEmpty(),
                "A full-damage anvil repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            // full damage chance degrades the pristine anvil
            helper.assertFalse(world.getBlockState(absPos).isOf(Blocks.ANVIL),
                "Anvil with damage_chance=1.0 should degrade on first use");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // cheap names with material

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesWithMaterial(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(100);
        sword.set(DataComponentTypes.REPAIR_COST, 10);
        // slot one contains material, not only a rename
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        ETTestHelper.setAnvilNewName(handler, "Test Sword");
        handler.updateResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "CheapNames repair-and-rename must produce an output");
            helper.assertTrue(handler.getLevelCost() > 1,
                "CheapNames should NOT force cost to 1 when slot 1 has material (got " + handler.getLevelCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairDamagedToChipped(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        BlockState damaged = Blocks.DAMAGED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.NORTH);
        helper.setBlockState(anvilPos, damaged);
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertTrue(player.isSneaking(), "repair player must be sneaking for the positive path");
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            ActionResult result = player.interactionManager.interactBlock(player, world, player.getMainHandStack(),
                Hand.MAIN_HAND, hit);
            helper.assertTrue(result == ActionResult.SUCCESS,
                "Repair interaction should return SUCCESS (got " + result + ")");
            BlockState after = world.getBlockState(absPos);
            helper.assertTrue(after.isOf(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after repair (got " + after + ")");
            helper.assertTrue(after.get(AnvilBlock.FACING) == Direction.NORTH,
                "Facing direction should be preserved after repair");
            helper.assertTrue(player.getMainHandStack().getCount() == 0,
                "Should consume 9 iron ingots (got " + player.getMainHandStack().getCount() + " remaining)");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairChippedToPristine(TestContext helper) {
        // cost nine allows one iron block
        // iron blocks contain nine ingots
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        BlockState chipped = Blocks.CHIPPED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.EAST);
        helper.setBlockState(anvilPos, chipped);
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_BLOCK, 1));
        try {
            helper.assertTrue(player.isSneaking(), "repair player must be sneaking for the positive path");
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            ActionResult result = player.interactionManager.interactBlock(player, world, player.getMainHandStack(),
                Hand.MAIN_HAND, hit);
            helper.assertTrue(result == ActionResult.SUCCESS,
                "Repair interaction should return SUCCESS (got " + result + ")");
            BlockState after = world.getBlockState(absPos);
            helper.assertTrue(after.isOf(Blocks.ANVIL),
                "Chipped anvil should become pristine after repair (got " + after + ")");
            helper.assertTrue(after.get(AnvilBlock.FACING) == Direction.EAST,
                "Facing direction should be preserved after repair");
            helper.assertTrue(player.getMainHandStack().isEmpty(), "Should consume 1 iron block");
            // successful repairs cancel vanilla iron block placement
            helper.assertFalse(world.getBlockState(absPos.up()).isOf(Blocks.IRON_BLOCK),
                "Iron block should not be placed when repairing");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairPristineNoop(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertTrue(player.isSneaking(), "repair player must be sneaking for the positive path");
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.ANVIL), "Pristine anvil should remain unchanged");
            helper.assertTrue(player.getMainHandStack().getCount() == 9,
                "No iron should be consumed on pristine anvil");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairDisabled(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", false);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        // iron ingots cannot be placed
        // vanilla pass leaves the anvil unchanged
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
        player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
        try {
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Damaged anvil should remain unchanged when feature is disabled");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairNotSneaking(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(false); // not sneaking must return pass
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertFalse(player.isSneaking(), "negative-path player must not be sneaking");
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged when player is not sneaking");
            helper.assertTrue(player.getMainHandStack().getCount() == 9,
                "No iron should be consumed when not sneaking (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairSpectatorGate(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        PlayerEntity player = helper.createMockPlayer(GameMode.SPECTATOR);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertTrue(player.isSpectator(),
                "createMockPlayer(SPECTATOR) should report isSpectator()==true (validates the mock)");
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            ActionResult result = UseBlockCallback.EVENT.invoker().interact(player, world, Hand.MAIN_HAND, hit);
            helper.assertTrue(result == ActionResult.PASS,
                "Spectator interaction must PASS (repair gated off) (got " + result + ")");
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Spectator must not repair the anvil (should remain damaged)");
            helper.assertTrue(player.getMainHandStack().getCount() == 9,
                "No iron should be consumed by a spectator (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairCreativeKeepsIron(TestContext helper) {
        // creative repairs do not consume iron
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            ActionResult result = player.interactionManager.interactBlock(player, world, player.getMainHandStack(),
                Hand.MAIN_HAND, hit);
            helper.assertTrue(result == ActionResult.SUCCESS,
                "Creative repair interaction should return SUCCESS (got " + result + ")");
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after creative repair");
            helper.assertTrue(player.getMainHandStack().getCount() == 9,
                "Creative repair should NOT consume iron (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairIngotNonMultipleCost(TestContext helper) {
        // ingots pay nonmultiple costs directly
        // iron blocks require multiples of nine
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "8");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 8));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            ActionResult result = player.interactionManager.interactBlock(player, world, player.getMainHandStack(),
                Hand.MAIN_HAND, hit);
            helper.assertTrue(result == ActionResult.SUCCESS,
                "Ingot repair at cost 8 should return SUCCESS (got " + result + ")");
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.CHIPPED_ANVIL),
                "8 ingots at cost 8 should repair the damaged anvil to chipped");
            helper.assertTrue(player.getMainHandStack().getCount() == 0,
                "Should consume all 8 ingots (got " + player.getMainHandStack().getCount() + " remaining)");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairOffHandGate(TestContext helper) {
        // only main-hand use triggers anvil repair
        // off-hand use leaves the anvil unchanged
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("anvil_repair", "anvil_repair_ingot_cost");
        try {
            ETTestHelper.setFeature("anvil_repair", true);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
            ServerWorld world = helper.getWorld();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
            player.setSneaking(true);
            player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 9));

            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getOffHandStack(), Hand.OFF_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Off-hand interaction must not repair the anvil");
            helper.assertTrue(player.getOffHandStack().getCount() == 9,
                "No iron should be consumed on an off-hand interaction (got " + player.getOffHandStack().getCount()
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairWrongItem(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        // non-iron items must pass without consumption
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.DIAMOND, 5));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged when holding the wrong item");
            helper.assertTrue(player.getMainHandStack().getCount() == 5,
                "Wrong item should not be consumed (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairCostZeroDisablesBoth(TestContext helper) {
        // zero cost disables ingot and block repairs
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "0");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        // the stone blocks vanilla item placement
        // this isolates the handler pass behavior
        helper.setBlockState(anvilPos.up(), Blocks.STONE.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);

            // iron ingots cannot repair at zero cost
            player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Ingot repair should be disabled when anvil_repair_ingot_cost=0");
            helper.assertTrue(player.getMainHandStack().getCount() == 9,
                "No ingots should be consumed when repair is disabled (got " + player.getMainHandStack().getCount()
                    + ")");

            // iron blocks cannot repair at zero cost
            player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_BLOCK, 2));
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Block repair should be disabled when anvil_repair_ingot_cost=0");
            helper.assertTrue(player.getMainHandStack().getCount() == 2,
                "No iron blocks should be consumed when repair is disabled (got " + player.getMainHandStack().getCount()
                    + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairInsufficientIron(TestContext helper) {
        // insufficient ingots prevent repair without consumption
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 5)); // below
                                                                                                                // cost
                                                                                                                // nine
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged with insufficient iron (5 < cost 9)");
            helper.assertTrue(player.getMainHandStack().getCount() == 5,
                "Partial iron amount should not be consumed (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairIngotExcessConsumesExact(TestContext helper) {
        // eighteen ingots repair one stage and leave nine
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 18));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after repair");
            helper.assertTrue(player.getMainHandStack().getCount() == 9,
                "Should consume exactly 9 ingots and leave 9 (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairBlockMultipleConsumesExact(TestContext helper) {
        // cost eighteen requires two iron blocks
        // three blocks leave one remaining
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "18");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        BlockState damaged = Blocks.DAMAGED_ANVIL.getDefaultState().with(AnvilBlock.FACING, Direction.SOUTH);
        helper.setBlockState(anvilPos, damaged);
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_BLOCK, 3));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            ActionResult result = player.interactionManager.interactBlock(player, world, player.getMainHandStack(),
                Hand.MAIN_HAND, hit);
            helper.assertTrue(result == ActionResult.SUCCESS,
                "Repair interaction should return SUCCESS (got " + result + ")");
            BlockState after = world.getBlockState(absPos);
            helper.assertTrue(after.isOf(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after block repair (got " + after + ")");
            helper.assertTrue(after.get(AnvilBlock.FACING) == Direction.SOUTH,
                "Facing direction should be preserved after repair");
            helper.assertTrue(player.getMainHandStack().getCount() == 1,
                "cost 18 should consume exactly 2 iron blocks and leave 1 (got " + player.getMainHandStack().getCount()
                    + ")");
            // successful repairs cancel vanilla iron block placement
            helper.assertFalse(world.getBlockState(absPos.up()).isOf(Blocks.IRON_BLOCK),
                "Iron block should not be placed when repairing");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairBlockCostNotMultipleNoop(TestContext helper) {
        // iron blocks require costs divisible by nine
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "8");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        // stone prevents vanilla block placement
        // this preserves the held block count
        helper.setBlockState(anvilPos.up(), Blocks.STONE.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_BLOCK, 3));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Iron block must not repair when the cost (8) is not a multiple of 9");
            helper.assertTrue(player.getMainHandStack().getCount() == 3,
                "No iron blocks should be consumed for a non-multiple cost (got " + player.getMainHandStack().getCount()
                    + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairBlockInsufficientNoop(TestContext helper) {
        // cost eighteen needs two blocks
        // one block cannot repair
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "18");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        // stone prevents vanilla block placement
        // this preserves the held block count
        helper.setBlockState(anvilPos.up(), Blocks.STONE.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_BLOCK, 1));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged with fewer blocks than required (1 < 2)");
            helper.assertTrue(player.getMainHandStack().getCount() == 1,
                "Insufficient iron blocks should not be consumed (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesPriorWorkFreeRenameStaysOne(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", true);
        ETTestHelper.setFeature("prior_work_free", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // sword with a prior-work penalty, pure rename (second slot empty)
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponentTypes.REPAIR_COST, 4);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        ETTestHelper.setAnvilNewName(handler, "Renamed Sword");
        handler.updateResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "CheapNames and PriorWorkFree rename must produce an output");
            helper.assertTrue(handler.getLevelCost() == 1,
                "Pure rename with cheap_names + prior_work_free both enabled should cost exactly 1 (got "
                    + handler.getLevelCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperOutputRepairCostEnabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "1.0");
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(200);
        sword.set(DataComponentTypes.REPAIR_COST, 4);
        try {
            ItemStack out = anvilMergeOutput(player, sword, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out.isEmpty(), "Repair output should be present");
            int rc = out.getOrDefault(DataComponentTypes.REPAIR_COST, -1);
            helper.assertTrue(rc == 5, "Output REPAIR_COST should be round(1.0*4+1)=5 (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperOutputRepairCostDisabled(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(200);
        sword.set(DataComponentTypes.REPAIR_COST, 4);
        try {
            ItemStack out = anvilMergeOutput(player, sword, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out.isEmpty(), "Repair output should be present");
            int rc = out.getOrDefault(DataComponentTypes.REPAIR_COST, -1);
            helper.assertTrue(rc == 9, "Vanilla output REPAIR_COST should be getNextCost(4)=9 (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperOutputRepairCostNonNegative(TestContext helper) {
        // large multipliers must clamp repair cost to integer max
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "600000000");
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(200);
        sword.set(DataComponentTypes.REPAIR_COST, 4);
        try {
            ItemStack out = anvilMergeOutput(player, sword, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out.isEmpty(), "Repair output should be present");
            int rc = out.getOrDefault(DataComponentTypes.REPAIR_COST, -1);
            helper.assertTrue(rc >= 0, "Stamped REPAIR_COST must never be negative (got " + rc + ")");
            helper.assertTrue(rc == Integer.MAX_VALUE,
                "A huge multiplier should clamp REPAIR_COST to Integer.MAX_VALUE (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperNonNumericMultiplier(TestContext helper) {
        // non-numeric multipliers use vanilla coefficient two
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "abc");
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // unparseable values use coefficient two and return nine
            helper.assertTrue(result == 9,
                "Non-numeric multiplier should fall back to 2.0 -> getNextCost(4)=9 (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    // prior work free removes both penalties

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeCombineBothSlots(TestContext helper) {
        // combining swords adds both prior-work penalties
        // prior work free removes both penalties
        // four plus eight equals twelve
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ETTestHelper.setFeature("prior_work_free", false);
        int fullCost;
        {
            ItemStack a = new ItemStack(Items.DIAMOND_SWORD);
            a.setDamage(500);
            a.set(DataComponentTypes.REPAIR_COST, 4);
            ItemStack b = new ItemStack(Items.DIAMOND_SWORD);
            b.set(DataComponentTypes.REPAIR_COST, 8);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, a, b);
            h.updateResult();
            fullCost = h.getLevelCost();
        }
        ETTestHelper.setFeature("prior_work_free", true);
        try {
            helper.assertTrue(fullCost > 12,
                "Baseline combine cost should exceed the 12-level prior-work penalty (got " + fullCost + ")");
            ItemStack a = new ItemStack(Items.DIAMOND_SWORD);
            a.setDamage(500);
            a.set(DataComponentTypes.REPAIR_COST, 4);
            ItemStack b = new ItemStack(Items.DIAMOND_SWORD);
            b.set(DataComponentTypes.REPAIR_COST, 8);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, a, b);
            h.updateResult();
            helper.assertTrue(h.getLevelCost() == fullCost - 12,
                "PriorWorkFree should subtract BOTH slots' REPAIR_COST (4+8=12): expected " + (fullCost - 12) + " got "
                    + h.getLevelCost());
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeRestoresHighPenaltyOutput(TestContext helper) {
        ETTestHelper.setFeature("prior_work_free", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack first = new ItemStack(Items.DIAMOND_SWORD);
            first.setDamage(500);
            first.set(DataComponentTypes.REPAIR_COST, 31);
            ItemStack second = new ItemStack(Items.DIAMOND_SWORD);
            second.setDamage(500);
            second.set(DataComponentTypes.REPAIR_COST, 31);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, first, second);
            handler.updateResult();

            helper.assertFalse(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "Removing the prior-work penalty before vanilla's threshold must keep a valid output");
            helper.assertTrue(handler.getLevelCost() < 40,
                "The repaired operation should no longer be too expensive (got " + handler.getLevelCost() + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeRenameFallThrough(TestContext helper) {
        // with cheap names off, prior work free affects renames
        // the ordering guard must not block the reduction
        ETTestHelper.setFeature("cheap_names", false);
        ETTestHelper.setFeature("prior_work_free", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponentTypes.REPAIR_COST, 4);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(h, "X");
            h.updateResult();
            // vanilla renames cost prior work plus one
            // prior work free removes the four-level penalty
            helper.assertFalse(h.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "PriorWorkFree rename must produce an output");
            helper.assertTrue(h.getLevelCost() == 1,
                "Pure rename with prior_work_free (cheap_names off) should be 5-4=1 (got " + h.getLevelCost() + ")");

            ETTestHelper.setFeature("prior_work_free", false);
            ItemStack sword2 = new ItemStack(Items.DIAMOND_SWORD);
            sword2.set(DataComponentTypes.REPAIR_COST, 4);
            AnvilScreenHandler h2 = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h2, sword2, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(h2, "X");
            h2.updateResult();
            helper.assertFalse(h2.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "Vanilla rename must produce an output");
            helper.assertTrue(h2.getLevelCost() == 5,
                "With prior_work_free off the penalty stays: 4+1=5 (got " + h2.getLevelCost() + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    // not too expensive inclusive cap boundary

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void notTooExpensiveInclusiveBoundary(TestContext helper) {
        ETTestHelper.setFeature("not_too_expensive", true);
        ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        int cost;
        {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(200);
            sword.set(DataComponentTypes.REPAIR_COST, 20);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.updateResult();
            cost = h.getLevelCost();
        }
        try {
            helper.assertTrue(cost > 0, "Baseline cost should be positive (got " + cost + ")");

            // cap == cost: inclusive >= must block (output empty)
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(cost));
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(200);
            sword.set(DataComponentTypes.REPAIR_COST, 20);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.updateResult();
            helper.assertTrue(h.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "cost==cap should block (>= is inclusive): cost=" + cost);

            // cap == cost + 1: must allow (output non-empty)
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(cost + 1));
            ItemStack sword2 = new ItemStack(Items.DIAMOND_SWORD);
            sword2.setDamage(200);
            sword2.set(DataComponentTypes.REPAIR_COST, 20);
            AnvilScreenHandler h2 = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h2, sword2, new ItemStack(Items.DIAMOND));
            h2.updateResult();
            helper.assertFalse(h2.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "cost < cap (cap=cost+1) should allow: cost=" + cost);
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
        }
        helper.complete();
    }

    // cheap names applies through take-output gameplay

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesRealTakeOutput(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", true);
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        player.addExperienceLevels(5);
        try {
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponentTypes.REPAIR_COST, 10);
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            boolean changed = handler.setNewItemName("Foo");
            handler.updateResult(); // recompute explicitly after setting the new name
            helper.assertTrue(changed, "setNewItemName should report a change");
            helper.assertTrue(handler.getLevelCost() == 1,
                "CheapNames pure rename should cost 1 (got " + handler.getLevelCost() + ")");
            ItemStack out = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            helper.assertFalse(out.isEmpty(), "Renamed output should be present");
            Text name = out.get(DataComponentTypes.CUSTOM_NAME);
            helper.assertTrue(name != null && name.getString().equals("Foo"),
                "Output should be renamed to Foo (got " + (name == null ? "null" : name.getString()) + ")");
            helper.assertTrue(invokeCanTakeOutput(handler, player),
                "A 5-level player should be able to take a 1-level rename");
            ETTestHelper.invokeOnTakeOutput(handler, player, out);
            helper.assertTrue(player.experienceLevel == 4,
                "Taking a 1-level rename should leave the player at level 4 (got " + player.experienceLevel + ")");
            helper.assertTrue(handler.getSlot(AnvilScreenHandler.INPUT_1_ID).getStack().isEmpty(),
                "Input slot 0 should be empty after taking the renamed item");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesVanillaCannotTake(TestContext helper) {
        // without cheap names, this rename costs eleven levels
        // five levels cannot take the output
        ETTestHelper.setFeature("cheap_names", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        player.addExperienceLevels(5);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponentTypes.REPAIR_COST, 10);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(handler, "Foo");
            handler.updateResult();
            helper.assertTrue(handler.getLevelCost() == 11,
                "Vanilla rename with REPAIR_COST=10 should cost 11 (got " + handler.getLevelCost() + ")");
            helper.assertFalse(invokeCanTakeOutput(handler, player),
                "A 5-level player must not be able to take an 11-level vanilla rename");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesClearNameRemovesCustomName(TestContext helper) {
        // clearing a custom name still creates an operation
        // cheap names makes it cost one
        ETTestHelper.setFeature("cheap_names", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Old"));
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(handler, "");
            handler.updateResult();
            helper.assertTrue(handler.getLevelCost() == 1,
                "CheapNames should force a clear-name to cost 1 (got " + handler.getLevelCost() + ")");
            ItemStack out = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            helper.assertFalse(out.isEmpty(), "Clearing a name must still produce an output");
            helper.assertFalse(out.contains(DataComponentTypes.CUSTOM_NAME),
                "Clearing the name should leave the output without a CUSTOM_NAME");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void cheapNamesSlot1EmptyNoRename(TestContext helper) {
        // an empty second slot without renaming has no operation
        // cheap names preserves vanilla output and cost
        ETTestHelper.setFeature("cheap_names", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            handler.updateResult();
            helper.assertTrue(handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "No rename and no material should produce an empty output");
            helper.assertTrue(handler.getLevelCost() == 0,
                "CheapNames should retain vanilla zero cost when no operation exists (got " + handler.getLevelCost()
                    + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.complete();
    }

    // sturdy anvils damaged-anvil destroy branch

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsDamagedDestroyed(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "1.0");
            ServerWorld world = helper.getWorld();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            player.addExperienceLevels(10000);
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.updateResult();
            ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            helper.assertFalse(result.isEmpty(),
                "A damaged-anvil repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            helper.assertTrue(world.getBlockState(absPos).isAir(),
                "A DAMAGED_ANVIL with damage_chance=1.0 should be destroyed (air) on use");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // disabled enchantments are stripped during anvil merges
    // zero maximum level removes the enchantment

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsAnvilStrip(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("disable_enchantments_enabled",
            "disable_enchantments");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_SWORD),
                enchantedBook(Enchantments.SHARPNESS, 3));
            helper.assertTrue(out.isEmpty(), "A disabled enchantment merge should produce no output");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsAnvilStripDisabled(TestContext helper) {
        ETTestHelper.setFeature("disable_enchantments_enabled", false);
        ETTestHelper.setConfigValue("disable_enchantments", "");
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_SWORD),
                enchantedBook(Enchantments.SHARPNESS, 3));
            helper.assertFalse(out.isEmpty(), "A normal enchantment merge should produce an output");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out) == 3,
                "Sharpness should merge normally when not disabled (got "
                    + EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out) + ")");
        } finally {
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        helper.complete();
    }

    // god armor merges protection enchantments

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godArmorAnvilMerge(TestContext helper) {
        ETTestHelper.setFeature("god_armor", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.addEnchantment(Enchantments.PROTECTION, 4);
            ItemStack out = anvilMergeOutput(player, chest, enchantedBook(Enchantments.BLAST_PROTECTION, 4));
            helper.assertFalse(out.isEmpty(), "God Armor merge output should be present");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.PROTECTION, out) == 4,
                "Output should keep Protection IV (got " + EnchantmentHelper.getLevel(Enchantments.PROTECTION, out)
                    + ")");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, out) == 4,
                "Output should gain Blast Protection IV (got "
                    + EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, out) + ")");
        } finally {
            ETTestHelper.setFeature("god_armor", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godArmorAnvilMergeDisabled(TestContext helper) {
        ETTestHelper.setFeature("god_armor", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.addEnchantment(Enchantments.PROTECTION, 4);
            ItemStack out = anvilMergeOutput(player, chest, enchantedBook(Enchantments.BLAST_PROTECTION, 4));
            helper.assertTrue(out.isEmpty(),
                "Without God Armor, incompatible protection enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("god_armor", false);
        }
        helper.complete();
    }

    // god weapons merges damage enchantments

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsAnvilMerge(TestContext helper) {
        ETTestHelper.setFeature("god_weapons", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.addEnchantment(Enchantments.SHARPNESS, 3);
            ItemStack out = anvilMergeOutput(player, sword, enchantedBook(Enchantments.SMITE, 2));
            helper.assertFalse(out.isEmpty(), "God Weapons merge output should be present");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out) == 3,
                "Output should keep Sharpness III (got " + EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out)
                    + ")");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SMITE, out) == 2,
                "Output should gain Smite II (got " + EnchantmentHelper.getLevel(Enchantments.SMITE, out) + ")");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsAnvilMergeDisabled(TestContext helper) {
        ETTestHelper.setFeature("god_weapons", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.addEnchantment(Enchantments.SHARPNESS, 3);
            ItemStack out = anvilMergeOutput(player, sword, enchantedBook(Enchantments.SMITE, 2));
            helper.assertTrue(out.isEmpty(),
                "Without God Weapons, incompatible damage enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsTridentCrossTweak(TestContext helper) {
        // trident weapons allows sharpness on tridents
        // god weapons permits sharpness with impaling
        ETTestHelper.setFeature("god_weapons", true);
        ETTestHelper.setFeature("trident_weapons", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack trident = new ItemStack(Items.TRIDENT);
            trident.addEnchantment(Enchantments.IMPALING, 3);
            ItemStack out = anvilMergeOutput(player, trident, enchantedBook(Enchantments.SHARPNESS, 2));
            helper.assertFalse(out.isEmpty(), "Cross-tweak merge output should be present");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.IMPALING, out) == 3,
                "Output should keep Impaling III (got " + EnchantmentHelper.getLevel(Enchantments.IMPALING, out) + ")");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out) == 2,
                "Output should gain Sharpness II (got " + EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out)
                    + ")");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.complete();
    }

    // multishot and piercing merge through anvils

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void multishotPiercingAnvilMerge(TestContext helper) {
        ETTestHelper.setFeature("multishot_piercing", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.addEnchantment(Enchantments.MULTISHOT, 1);
            ItemStack out = anvilMergeOutput(player, crossbow, enchantedBook(Enchantments.PIERCING, 1));
            helper.assertFalse(out.isEmpty(), "Multishot Piercing merge output should be present");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MULTISHOT, out) == 1,
                "Output should keep Multishot I (got " + EnchantmentHelper.getLevel(Enchantments.MULTISHOT, out) + ")");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.PIERCING, out) == 1,
                "Output should gain Piercing I (got " + EnchantmentHelper.getLevel(Enchantments.PIERCING, out) + ")");
        } finally {
            ETTestHelper.setFeature("multishot_piercing", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void multishotPiercingAnvilMergeDisabled(TestContext helper) {
        ETTestHelper.setFeature("multishot_piercing", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.addEnchantment(Enchantments.MULTISHOT, 1);
            ItemStack out = anvilMergeOutput(player, crossbow, enchantedBook(Enchantments.PIERCING, 1));
            helper.assertTrue(out.isEmpty(),
                "Without Multishot Piercing, incompatible enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("multishot_piercing", false);
        }
        helper.complete();
    }

    // trident weapons merge through anvils

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsAnvilMerge(TestContext helper) {
        ETTestHelper.setFeature("trident_weapons", true);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack outFa = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.FIRE_ASPECT, 2));
            helper.assertFalse(outFa.isEmpty(), "Fire Aspect transfer should produce an output");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, outFa) == 2,
                "Fire Aspect II should transfer onto a trident (got "
                    + EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, outFa) + ")");
            ItemStack outKb = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.KNOCKBACK, 2));
            helper.assertFalse(outKb.isEmpty(), "Knockback transfer should produce an output");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, outKb) == 2,
                "Knockback II should transfer onto a trident (got "
                    + EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, outKb) + ")");
            ItemStack outLo = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.LOOTING, 3));
            helper.assertFalse(outLo.isEmpty(), "Looting transfer should produce an output");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.LOOTING, outLo) == 3,
                "Looting III should transfer onto a trident (got "
                    + EnchantmentHelper.getLevel(Enchantments.LOOTING, outLo) + ")");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsAnvilMergeDisabled(TestContext helper) {
        ETTestHelper.setFeature("trident_weapons", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.FIRE_ASPECT, 2));
            helper.assertTrue(out.isEmpty(),
                "Without Trident Weapons, unsupported enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.complete();
    }

    // axe weapons precedence with disabled enchantments

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axeWeaponsAnvilDisabled(TestContext helper) {
        ETTestHelper.setFeature("axe_weapons", false);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_AXE),
                enchantedBook(Enchantments.LOOTING, 3));
            helper.assertTrue(out.isEmpty(), "Without Axe Weapons, unsupported enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axeWeaponsDisableEnchantPrecedence(TestContext helper) {
        // disabled enchantments override axe weapons
        ETTestHelper.setFeature("axe_weapons", true);
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "looting");
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_AXE),
                enchantedBook(Enchantments.LOOTING, 3));
            helper.assertTrue(out.isEmpty(), "Disabled enchantments must prevent the axe merge output");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
            ETTestHelper.setConfigValue("disable_enchantments", "");
        }
        helper.complete();
    }

    // raised caps apply during anvil merges

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodAnvilClamp(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, enchantedBook(Enchantments.SHARPNESS, 5),
                enchantedBook(Enchantments.SHARPNESS, 5));
            helper.assertFalse(out.isEmpty(), "Combining two Sharpness books should produce output");
            helper.assertTrue(bookOrItemLevel(Enchantments.SHARPNESS, out) == 6,
                "With the cap raised to 10, two Sharpness-V books should combine to VI (got "
                    + bookOrItemLevel(Enchantments.SHARPNESS, out) + ")");
        } finally {
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodAnvilClampDisabled(TestContext helper) {
        ETTestHelper.setCapmod(false);
        ETTestHelper.setEnchantCap("sharpness", -1);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, enchantedBook(Enchantments.SHARPNESS, 5),
                enchantedBook(Enchantments.SHARPNESS, 5));
            helper.assertFalse(out.isEmpty(), "Vanilla Sharpness combine should produce output");
            // book outputs use stored enchantments
            // book or item level reads them
            helper.assertTrue(bookOrItemLevel(Enchantments.SHARPNESS, out) == 5,
                "Without capmod the anvil clamps the combine to the vanilla max of 5 (got "
                    + bookOrItemLevel(Enchantments.SHARPNESS, out) + ")");
        } finally {
            ETTestHelper.setCapmod(false);
        }
        helper.complete();
    }

    // edge cases

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperFractionalRounding(TestContext helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "0.5");
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            // repair cost three yields round two point five
            // half-up rounding produces three
            ItemStack sword3 = new ItemStack(Items.DIAMOND_SWORD);
            sword3.setDamage(200);
            sword3.set(DataComponentTypes.REPAIR_COST, 3);
            ItemStack out3 = anvilMergeOutput(player, sword3, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out3.isEmpty(), "Fractional rounding repair should produce an output");
            int rc3 = out3.getOrDefault(DataComponentTypes.REPAIR_COST, -1);
            helper.assertTrue(rc3 == 3, "round(0.5*3+1)=round(2.5) must be 3 (half-up), not 2 (got " + rc3 + ")");

            // round one point five yields two
            ItemStack sword1 = new ItemStack(Items.DIAMOND_SWORD);
            sword1.setDamage(200);
            sword1.set(DataComponentTypes.REPAIR_COST, 1);
            ItemStack out1 = anvilMergeOutput(player, sword1, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out1.isEmpty(), "Second fractional rounding repair should produce an output");
            int rc1 = out1.getOrDefault(DataComponentTypes.REPAIR_COST, -1);
            helper.assertTrue(rc1 == 2, "round(0.5*1+1)=round(1.5) must be 2 (got " + rc1 + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkFreeCheapNamesCombineStillReduced(TestContext helper) {
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ETTestHelper.setFeature("cheap_names", true);
        ETTestHelper.setFeature("prior_work_free", false);
        int fullCost;
        {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(500);
            sword.set(DataComponentTypes.REPAIR_COST, 8);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.updateResult();
            fullCost = h.getLevelCost();
        }
        ETTestHelper.setFeature("prior_work_free", true);
        try {
            helper.assertTrue(fullCost > 8,
                "Baseline combine cost should exceed the 8-level prior-work penalty (got " + fullCost + ")");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(500);
            sword.set(DataComponentTypes.REPAIR_COST, 8);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.updateResult();
            helper.assertTrue(h.getLevelCost() == fullCost - 8,
                "On a combine, PriorWorkFree must still subtract the penalty even with cheap_names on: expected "
                    + (fullCost - 8) + " got " + h.getLevelCost());
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void priorWorkCheaperAndFreeCompose(TestContext helper) {
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // baseline cost uses a repair without prior-work penalties
        ETTestHelper.setFeature("prior_work_free", false);
        ETTestHelper.setFeature("prior_work_cheaper", false);
        int baseCost;
        {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(200);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.updateResult();
            baseCost = h.getLevelCost();
        }
        ETTestHelper.setFeature("prior_work_free", true);
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "1.0");
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(200);
            sword.set(DataComponentTypes.REPAIR_COST, 4);
            AnvilScreenHandler h = new AnvilScreenHandler(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.updateResult();
            // prior work free removes the four-level penalty
            // this matches the no-penalty baseline
            helper.assertTrue(h.getLevelCost() == baseCost, "PriorWorkFree should remove the prior-work penalty (base="
                + baseCost + " got=" + h.getLevelCost() + ")");
            // prior work cheaper stamps repair cost five
            // vanilla would stamp nine
            helper.assertFalse(h.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().isEmpty(),
                "PriorWorkCheaper repair should produce an output");
            int rc = h.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack().getOrDefault(DataComponentTypes.REPAIR_COST,
                -1);
            helper.assertTrue(rc == 5, "PriorWorkCheaper should stamp REPAIR_COST=round(1.0*4+1)=5 (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsNegativeChanceClampsToZero(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "-5.0");
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
            ServerWorld world = helper.getWorld();
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            player.addExperienceLevels(10000);
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            for (int i = 0; i < 10; i++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.setDamage(100);
                ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
                handler.updateResult();
                ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
                helper.assertFalse(result.isEmpty(),
                    "A negative-chance repair setup must produce an output before onTakeOutput");
                ETTestHelper.invokeOnTakeOutput(handler, player, result);
            }
            helper.expectBlock(Blocks.ANVIL, anvilPos);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsAboveOneClampsToOne(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "5.0");
            ServerWorld world = helper.getWorld();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            player.addExperienceLevels(10000);
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.updateResult();
            ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            helper.assertFalse(result.isEmpty(),
                "An above-one-chance repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            helper.assertFalse(world.getBlockState(absPos).isOf(Blocks.ANVIL),
                "anvil_damage_chance=5.0 should clamp to 1.0 and degrade the anvil on first use");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void sturdyAnvilsCreativeNeverDamaged(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "1.0");
            ServerWorld world = helper.getWorld();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlockState(anvilPos, Blocks.ANVIL.getDefaultState());
            BlockPos absPos = helper.getAbsolutePos(anvilPos);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            player.addExperienceLevels(10000);
            player.getAbilities().creativeMode = true; // vanilla checks creative mode
            ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
            AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory(), ctx);
            helper.assertTrue(player.isInCreativeMode(),
                "abilities.creativeMode must make isInCreativeMode() true (validates the setup)");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamage(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.updateResult();
            ItemStack result = handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
            helper.assertFalse(result.isEmpty(),
                "A creative-anvil repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.ANVIL),
                "A creative player must never damage the anvil, even at damage_chance=1.0");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairNegativeCostDisables(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "-5");
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        helper.setBlockState(anvilPos.up(), Blocks.STONE.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);

            player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 9));
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Negative cost should disable ingot repair");
            helper.assertTrue(player.getMainHandStack().getCount() == 9,
                "No ingots should be consumed with a negative cost (got " + player.getMainHandStack().getCount() + ")");

            player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_BLOCK, 2));
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "Negative cost should disable block repair");
            helper.assertTrue(player.getMainHandStack().getCount() == 2,
                "No iron blocks should be consumed with a negative cost (got " + player.getMainHandStack().getCount()
                    + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void anvilRepairHugeCostUnaffordable(TestContext helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", String.valueOf(Integer.MAX_VALUE));
        ServerWorld world = helper.getWorld();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlockState(anvilPos, Blocks.DAMAGED_ANVIL.getDefaultState());
        helper.setBlockState(anvilPos.up(), Blocks.STONE.getDefaultState());
        BlockPos absPos = helper.getAbsolutePos(anvilPos);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        player.setSneaking(true);
        try {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(absPos), Direction.UP, absPos, false);

            player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_INGOT, 64));
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "A max-int cost must be unaffordable with ingots");
            helper.assertTrue(player.getMainHandStack().getCount() == 64,
                "No ingots consumed at an unaffordable cost (got " + player.getMainHandStack().getCount() + ")");

            player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.IRON_BLOCK, 64));
            player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).isOf(Blocks.DAMAGED_ANVIL),
                "A max-int cost (not a multiple of 9) must be unaffordable with blocks");
            helper.assertTrue(player.getMainHandStack().getCount() == 64,
                "No iron blocks consumed at an unaffordable cost (got " + player.getMainHandStack().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.complete();
    }

    // helpers

    /**
     * builds an anvil handler and updates its result returns the output stack
     */
    private static ItemStack anvilMergeOutput(PlayerEntity player, ItemStack first, ItemStack second) {
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, first, second);
        handler.updateResult();
        return handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
    }

    /** creates an enchanted book with one stored enchantment */
    private static ItemStack enchantedBook(Enchantment enchantment, int level) {
        return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, level));
    }

    private static int bookOrItemLevel(Enchantment enchantment, ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).getLevel(enchantment);
    }

    /**
     * uses reflection to call protected can-take-output
     */
    private static boolean invokeCanTakeOutput(AnvilScreenHandler handler, PlayerEntity player) {
        try {
            Method m = AnvilScreenHandler.class.getDeclaredMethod("canTakeOutput", PlayerEntity.class, boolean.class);
            m.setAccessible(true);
            return (boolean) m.invoke(handler, player, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("canTakeOutput reflection failed", e);
        }
    }
}
