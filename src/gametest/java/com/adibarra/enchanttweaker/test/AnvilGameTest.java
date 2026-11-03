package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Method;
import java.util.Map;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.adibarra.enchanttweaker.ETMixinPlugin;

public class AnvilGameTest implements FabricGameTest {

    // cheap names applies to single-input renames
    // repair cost ten normally raises rename cost

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesEnabled(GameTestHelper helper) {
        ETTestHelper.setFeature("cheap_names", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.REPAIR_COST, 10);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        ETTestHelper.setAnvilNewName(handler, "Test Sword");
        handler.createResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "CheapNames rename must produce an output");
            helper.assertTrue(handler.getCost() == 1,
                "CheapNames should force rename cost to 1 (got " + handler.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("cheap_names", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.REPAIR_COST, 10);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        ETTestHelper.setAnvilNewName(handler, "Test Sword");
        handler.createResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "Vanilla rename must produce an output");
            helper.assertTrue(handler.getCost() > 1,
                "Vanilla rename with REPAIR_COST=10 should cost > 1 (got " + handler.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesEmptyOperationStaysEmpty(GameTestHelper helper) {
        ETTestHelper.setFeature("cheap_names", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.REPAIR_COST, 10);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        handler.createResult();
        try {
            helper.assertTrue(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "CheapNames must not create a result when no rename or second input was supplied");
            helper.assertTrue(handler.getCost() == 10,
                "CheapNames must not rewrite the vanilla cost of an empty operation (got " + handler.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    // not too expensive raises the vanilla cost cap
    // the configured maximum defaults to integer max
    // repair cost fifty exceeds forty levels

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void notTooExpensiveEnabled(GameTestHelper helper) {
        ETTestHelper.setFeature("not_too_expensive", true);
        ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(100);
        sword.set(DataComponents.REPAIR_COST, 50);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        handler.createResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "NotTooExpensive should allow repair beyond the 40-level cap (result was empty)");
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void notTooExpensiveDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("not_too_expensive", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(100);
        sword.set(DataComponents.REPAIR_COST, 50);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        handler.createResult();
        try {
            helper.assertTrue(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "Vanilla 40-level cap should block repair when not_too_expensive is disabled");
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
        }
        helper.succeed();
    }

    // sturdy anvils controls damage after taking output
    // the handler needs a real screen context
    // the context runs the take-output lambda

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void sturdyAnvilsNeverBreaks(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "0.0");
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
            ServerLevel world = helper.getLevel();
            BlockPos absPos = helper.absolutePos(anvilPos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(10000);
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            for (int i = 0; i < 10; i++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.setDamageValue(100);
                ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
                handler.createResult();
                ItemStack result = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                helper.assertFalse(result.isEmpty(),
                    "A sturdy-anvil repair setup must produce an output before onTakeOutput");
                ETTestHelper.invokeOnTakeOutput(handler, player, result);
            }
            helper.assertBlockPresent(Blocks.ANVIL, anvilPos);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void sturdyAnvilsDisabled(GameTestHelper helper) {
        // vanilla damage chance is twelve percent per use
        // a seeded random roll deterministically exercises the damage branch
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils");
        try {
            ETTestHelper.setFeature("sturdy_anvils", false);
            ServerLevel world = helper.getLevel();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
            BlockPos absPos = helper.absolutePos(anvilPos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(10000);
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.createResult();
            ItemStack result = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(result.isEmpty(),
                "A vanilla-anvil repair setup must produce an output before onTakeOutput");

            long damageSeed = 0L;
            while (damageSeed < 1_000_000L && net.minecraft.util.RandomSource.create(damageSeed).nextFloat() >= 0.12F)
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
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.CHIPPED_ANVIL),
                "vanilla anvil use with a deterministic damage roll should produce a chipped anvil");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // prior work free

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkFreeEnabled(GameTestHelper helper) {
        ETTestHelper.setFeature("prior_work_free", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // sword without prior work
        ItemStack swordNoPenalty = new ItemStack(Items.DIAMOND_SWORD);
        swordNoPenalty.setDamageValue(50);
        // matching sword with prior work
        ItemStack swordWithPenalty = swordNoPenalty.copy();
        swordWithPenalty.set(DataComponents.REPAIR_COST, 4);
        // calculate base cost without prior work
        AnvilMenu base = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(base, swordNoPenalty, new ItemStack(Items.DIAMOND));
        base.createResult();
        int baseCost = base.getCost();
        // enabled prior work free removes the penalty
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, swordWithPenalty, new ItemStack(Items.DIAMOND));
        handler.createResult();
        int penaltyCost = handler.getCost();
        try {
            helper.assertTrue(penaltyCost == baseCost,
                "Prior work cost should be removed when enabled (base=" + baseCost + " got=" + penaltyCost + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkFreeDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("prior_work_free", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // sword without prior work
        ItemStack swordNoPenalty = new ItemStack(Items.DIAMOND_SWORD);
        swordNoPenalty.setDamageValue(50);
        // matching sword with prior work
        ItemStack swordWithPenalty = swordNoPenalty.copy();
        swordWithPenalty.set(DataComponents.REPAIR_COST, 4);
        // calculate base cost without prior work
        AnvilMenu base = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(base, swordNoPenalty, new ItemStack(Items.DIAMOND));
        base.createResult();
        int baseCost = base.getCost();
        // disabled prior work free retains the penalty
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, swordWithPenalty, new ItemStack(Items.DIAMOND));
        handler.createResult();
        int penaltyCost = handler.getCost();
        try {
            helper.assertTrue(penaltyCost > baseCost,
                "Prior work cost should be included when disabled (base=" + baseCost + " got=" + penaltyCost + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.succeed();
    }

    // prior work cheaper

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperEnabled(GameTestHelper helper) {
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("prior_work_cheaper", false);
        try {
            int result = ETTestHelper.getNextAnvilCost(4);
            // vanilla multiplier yields nine
            helper.assertTrue(result == 9, "getNextCost(4) should be vanilla 9 when disabled (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperZeroMultiplier(GameTestHelper helper) {
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperHighMultiplier(GameTestHelper helper) {
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperNegativeMultiplier(GameTestHelper helper) {
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
        helper.succeed();
    }

    // not too expensive custom maximum cost

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void notTooExpensiveCustomMaxCost(GameTestHelper helper) {
        ETTestHelper.setFeature("not_too_expensive", true);
        ETTestHelper.setConfigValue("nte_max_cost", "50");
        ETMixinPlugin.clearCaches();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // repair cost sixty exceeds the configured cap
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(100);
        sword.set(DataComponents.REPAIR_COST, 60);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        handler.createResult();
        try {
            helper.assertTrue(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "Custom nte_max_cost=50 should block repair with cost > 50");
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
            ETMixinPlugin.clearCaches();
        }
        helper.succeed();
    }

    // sturdy anvils can always break

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void sturdyAnvilsAlwaysBreaks(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "1.0");
            ServerLevel world = helper.getLevel();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
            BlockPos absPos = helper.absolutePos(anvilPos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(10000);
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.createResult();
            ItemStack result = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(result.isEmpty(),
                "A full-damage anvil repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            // full damage chance degrades the pristine anvil
            helper.assertFalse(world.getBlockState(absPos).is(Blocks.ANVIL),
                "Anvil with damage_chance=1.0 should degrade on first use");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // cheap names with material

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesWithMaterial(GameTestHelper helper) {
        ETTestHelper.setFeature("cheap_names", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(100);
        sword.set(DataComponents.REPAIR_COST, 10);
        // slot one contains material, not only a rename
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
        ETTestHelper.setAnvilNewName(handler, "Test Sword");
        handler.createResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "CheapNames repair-and-rename must produce an output");
            helper.assertTrue(handler.getCost() > 1,
                "CheapNames should NOT force cost to 1 when slot 1 has material (got " + handler.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairDamagedToChipped(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        BlockState damaged = Blocks.DAMAGED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.NORTH);
        helper.setBlock(anvilPos, damaged);
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertTrue(player.isShiftKeyDown(), "repair player must be sneaking for the positive path");
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            InteractionResult result = player.gameMode.useItemOn(player, world, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result == InteractionResult.SUCCESS,
                "Repair interaction should return SUCCESS (got " + result + ")");
            BlockState after = world.getBlockState(absPos);
            helper.assertTrue(after.is(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after repair (got " + after + ")");
            helper.assertTrue(after.getValue(AnvilBlock.FACING) == Direction.NORTH,
                "Facing direction should be preserved after repair");
            helper.assertTrue(player.getMainHandItem().getCount() == 0,
                "Should consume 9 iron ingots (got " + player.getMainHandItem().getCount() + " remaining)");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairChippedToPristine(GameTestHelper helper) {
        // cost nine allows one iron block
        // iron blocks contain nine ingots
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        BlockState chipped = Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST);
        helper.setBlock(anvilPos, chipped);
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_BLOCK, 1));
        try {
            helper.assertTrue(player.isShiftKeyDown(), "repair player must be sneaking for the positive path");
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            InteractionResult result = player.gameMode.useItemOn(player, world, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result == InteractionResult.SUCCESS,
                "Repair interaction should return SUCCESS (got " + result + ")");
            BlockState after = world.getBlockState(absPos);
            helper.assertTrue(after.is(Blocks.ANVIL),
                "Chipped anvil should become pristine after repair (got " + after + ")");
            helper.assertTrue(after.getValue(AnvilBlock.FACING) == Direction.EAST,
                "Facing direction should be preserved after repair");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "Should consume 1 iron block");
            // successful repairs cancel vanilla iron block placement
            helper.assertFalse(world.getBlockState(absPos.above()).is(Blocks.IRON_BLOCK),
                "Iron block should not be placed when repairing");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairPristineNoop(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertTrue(player.isShiftKeyDown(), "repair player must be sneaking for the positive path");
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.ANVIL), "Pristine anvil should remain unchanged");
            helper.assertTrue(player.getMainHandItem().getCount() == 9, "No iron should be consumed on pristine anvil");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", false);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        // iron ingots cannot be placed
        // vanilla pass leaves the anvil unchanged
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
        player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        try {
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Damaged anvil should remain unchanged when feature is disabled");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairNotSneaking(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(false); // not sneaking must return pass
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertFalse(player.isShiftKeyDown(), "negative-path player must not be sneaking");
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged when player is not sneaking");
            helper.assertTrue(player.getMainHandItem().getCount() == 9,
                "No iron should be consumed when not sneaking (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairSpectatorGate(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        Player player = helper.makeMockPlayer(GameType.SPECTATOR);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
        try {
            helper.assertTrue(player.isSpectator(),
                "createMockPlayer(SPECTATOR) should report isSpectator()==true (validates the mock)");
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, world,
                InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result == InteractionResult.PASS,
                "Spectator interaction must PASS (repair gated off) (got " + result + ")");
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Spectator must not repair the anvil (should remain damaged)");
            helper.assertTrue(player.getMainHandItem().getCount() == 9,
                "No iron should be consumed by a spectator (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairCreativeKeepsIron(GameTestHelper helper) {
        // creative repairs do not consume iron
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            InteractionResult result = player.gameMode.useItemOn(player, world, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result == InteractionResult.SUCCESS,
                "Creative repair interaction should return SUCCESS (got " + result + ")");
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after creative repair");
            helper.assertTrue(player.getMainHandItem().getCount() == 9,
                "Creative repair should NOT consume iron (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairIngotNonMultipleCost(GameTestHelper helper) {
        // ingots pay nonmultiple costs directly
        // iron blocks require multiples of nine
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "8");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 8));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            InteractionResult result = player.gameMode.useItemOn(player, world, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result == InteractionResult.SUCCESS,
                "Ingot repair at cost 8 should return SUCCESS (got " + result + ")");
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.CHIPPED_ANVIL),
                "8 ingots at cost 8 should repair the damaged anvil to chipped");
            helper.assertTrue(player.getMainHandItem().getCount() == 0,
                "Should consume all 8 ingots (got " + player.getMainHandItem().getCount() + " remaining)");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairOffHandGate(GameTestHelper helper) {
        // only main-hand use triggers anvil repair
        // off-hand use leaves the anvil unchanged
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("anvil_repair", "anvil_repair_ingot_cost");
        try {
            ETTestHelper.setFeature("anvil_repair", true);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
            ServerLevel world = helper.getLevel();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
            BlockPos absPos = helper.absolutePos(anvilPos);
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 9));

            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getOffhandItem(), InteractionHand.OFF_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Off-hand interaction must not repair the anvil");
            helper.assertTrue(player.getOffhandItem().getCount() == 9,
                "No iron should be consumed on an off-hand interaction (got " + player.getOffhandItem().getCount()
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairWrongItem(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        // non-iron items must pass without consumption
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.DIAMOND, 5));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged when holding the wrong item");
            helper.assertTrue(player.getMainHandItem().getCount() == 5,
                "Wrong item should not be consumed (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairCostZeroDisablesBoth(GameTestHelper helper) {
        // zero cost disables ingot and block repairs
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "0");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        // the stone blocks vanilla item placement
        // this isolates the handler pass behavior
        helper.setBlock(anvilPos.above(), Blocks.STONE.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);

            // iron ingots cannot repair at zero cost
            player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Ingot repair should be disabled when anvil_repair_ingot_cost=0");
            helper.assertTrue(player.getMainHandItem().getCount() == 9,
                "No ingots should be consumed when repair is disabled (got " + player.getMainHandItem().getCount()
                    + ")");

            // iron blocks cannot repair at zero cost
            player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_BLOCK, 2));
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Block repair should be disabled when anvil_repair_ingot_cost=0");
            helper.assertTrue(player.getMainHandItem().getCount() == 2,
                "No iron blocks should be consumed when repair is disabled (got " + player.getMainHandItem().getCount()
                    + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairInsufficientIron(GameTestHelper helper) {
        // insufficient ingots prevent repair without consumption
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 5)); // below
                                                                                                           // cost
                                                                                                           // nine
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged with insufficient iron (5 < cost 9)");
            helper.assertTrue(player.getMainHandItem().getCount() == 5,
                "Partial iron amount should not be consumed (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairIngotExcessConsumesExact(GameTestHelper helper) {
        // eighteen ingots repair one stage and leave nine
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 18));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after repair");
            helper.assertTrue(player.getMainHandItem().getCount() == 9,
                "Should consume exactly 9 ingots and leave 9 (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairBlockMultipleConsumesExact(GameTestHelper helper) {
        // cost eighteen requires two iron blocks
        // three blocks leave one remaining
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "18");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        BlockState damaged = Blocks.DAMAGED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.SOUTH);
        helper.setBlock(anvilPos, damaged);
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_BLOCK, 3));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            InteractionResult result = player.gameMode.useItemOn(player, world, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(result == InteractionResult.SUCCESS,
                "Repair interaction should return SUCCESS (got " + result + ")");
            BlockState after = world.getBlockState(absPos);
            helper.assertTrue(after.is(Blocks.CHIPPED_ANVIL),
                "Damaged anvil should become chipped after block repair (got " + after + ")");
            helper.assertTrue(after.getValue(AnvilBlock.FACING) == Direction.SOUTH,
                "Facing direction should be preserved after repair");
            helper.assertTrue(player.getMainHandItem().getCount() == 1,
                "cost 18 should consume exactly 2 iron blocks and leave 1 (got " + player.getMainHandItem().getCount()
                    + ")");
            // successful repairs cancel vanilla iron block placement
            helper.assertFalse(world.getBlockState(absPos.above()).is(Blocks.IRON_BLOCK),
                "Iron block should not be placed when repairing");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairBlockCostNotMultipleNoop(GameTestHelper helper) {
        // iron blocks require costs divisible by nine
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "8");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        // stone prevents vanilla block placement
        // this preserves the held block count
        helper.setBlock(anvilPos.above(), Blocks.STONE.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_BLOCK, 3));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Iron block must not repair when the cost (8) is not a multiple of 9");
            helper.assertTrue(player.getMainHandItem().getCount() == 3,
                "No iron blocks should be consumed for a non-multiple cost (got " + player.getMainHandItem().getCount()
                    + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairBlockInsufficientNoop(GameTestHelper helper) {
        // cost eighteen needs two blocks
        // one block cannot repair
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "18");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        // stone prevents vanilla block placement
        // this preserves the held block count
        helper.setBlock(anvilPos.above(), Blocks.STONE.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_BLOCK, 1));
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Anvil should remain damaged with fewer blocks than required (1 < 2)");
            helper.assertTrue(player.getMainHandItem().getCount() == 1,
                "Insufficient iron blocks should not be consumed (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesPriorWorkFreeRenameStaysOne(GameTestHelper helper) {
        ETTestHelper.setFeature("cheap_names", true);
        ETTestHelper.setFeature("prior_work_free", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // sword with a prior-work penalty, pure rename (second slot empty)
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.REPAIR_COST, 4);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
        ETTestHelper.setAnvilNewName(handler, "Renamed Sword");
        handler.createResult();
        try {
            helper.assertFalse(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "CheapNames and PriorWorkFree rename must produce an output");
            helper.assertTrue(handler.getCost() == 1,
                "Pure rename with cheap_names + prior_work_free both enabled should cost exactly 1 (got "
                    + handler.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperOutputRepairCostEnabled(GameTestHelper helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "1.0");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(200);
        sword.set(DataComponents.REPAIR_COST, 4);
        try {
            ItemStack out = anvilMergeOutput(player, sword, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out.isEmpty(), "Repair output should be present");
            int rc = out.getOrDefault(DataComponents.REPAIR_COST, -1);
            helper.assertTrue(rc == 5, "Output REPAIR_COST should be round(1.0*4+1)=5 (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperOutputRepairCostDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("prior_work_cheaper", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(200);
        sword.set(DataComponents.REPAIR_COST, 4);
        try {
            ItemStack out = anvilMergeOutput(player, sword, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out.isEmpty(), "Repair output should be present");
            int rc = out.getOrDefault(DataComponents.REPAIR_COST, -1);
            helper.assertTrue(rc == 9, "Vanilla output REPAIR_COST should be getNextCost(4)=9 (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperOutputRepairCostNonNegative(GameTestHelper helper) {
        // large multipliers must clamp repair cost to integer max
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "600000000");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(200);
        sword.set(DataComponents.REPAIR_COST, 4);
        try {
            ItemStack out = anvilMergeOutput(player, sword, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out.isEmpty(), "Repair output should be present");
            int rc = out.getOrDefault(DataComponents.REPAIR_COST, -1);
            helper.assertTrue(rc >= 0, "Stamped REPAIR_COST must never be negative (got " + rc + ")");
            helper.assertTrue(rc == Integer.MAX_VALUE,
                "A huge multiplier should clamp REPAIR_COST to Integer.MAX_VALUE (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperNonNumericMultiplier(GameTestHelper helper) {
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
        helper.succeed();
    }

    // prior work free removes both penalties

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkFreeCombineBothSlots(GameTestHelper helper) {
        // combining swords adds both prior-work penalties
        // prior work free removes both penalties
        // four plus eight equals twelve
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ETTestHelper.setFeature("prior_work_free", false);
        int fullCost;
        {
            ItemStack a = new ItemStack(Items.DIAMOND_SWORD);
            a.setDamageValue(500);
            a.set(DataComponents.REPAIR_COST, 4);
            ItemStack b = new ItemStack(Items.DIAMOND_SWORD);
            b.set(DataComponents.REPAIR_COST, 8);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, a, b);
            h.createResult();
            fullCost = h.getCost();
        }
        ETTestHelper.setFeature("prior_work_free", true);
        try {
            helper.assertTrue(fullCost > 12,
                "Baseline combine cost should exceed the 12-level prior-work penalty (got " + fullCost + ")");
            ItemStack a = new ItemStack(Items.DIAMOND_SWORD);
            a.setDamageValue(500);
            a.set(DataComponents.REPAIR_COST, 4);
            ItemStack b = new ItemStack(Items.DIAMOND_SWORD);
            b.set(DataComponents.REPAIR_COST, 8);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, a, b);
            h.createResult();
            helper.assertTrue(h.getCost() == fullCost - 12,
                "PriorWorkFree should subtract BOTH slots' REPAIR_COST (4+8=12): expected " + (fullCost - 12) + " got "
                    + h.getCost());
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkFreeRestoresHighPenaltyOutput(GameTestHelper helper) {
        ETTestHelper.setFeature("prior_work_free", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack first = new ItemStack(Items.DIAMOND_SWORD);
            first.setDamageValue(500);
            first.set(DataComponents.REPAIR_COST, 31);
            ItemStack second = new ItemStack(Items.DIAMOND_SWORD);
            second.setDamageValue(500);
            second.set(DataComponents.REPAIR_COST, 31);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, first, second);
            handler.createResult();

            helper.assertFalse(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "Removing the prior-work penalty before vanilla's threshold must keep a valid output");
            helper.assertTrue(handler.getCost() < 40,
                "The repaired operation should no longer be too expensive (got " + handler.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkFreeRenameFallThrough(GameTestHelper helper) {
        // with cheap names off, prior work free affects renames
        // the ordering guard must not block the reduction
        ETTestHelper.setFeature("cheap_names", false);
        ETTestHelper.setFeature("prior_work_free", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.REPAIR_COST, 4);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(h, "X");
            h.createResult();
            // vanilla renames cost prior work plus one
            // prior work free removes the four-level penalty
            helper.assertFalse(h.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "PriorWorkFree rename must produce an output");
            helper.assertTrue(h.getCost() == 1,
                "Pure rename with prior_work_free (cheap_names off) should be 5-4=1 (got " + h.getCost() + ")");

            ETTestHelper.setFeature("prior_work_free", false);
            ItemStack sword2 = new ItemStack(Items.DIAMOND_SWORD);
            sword2.set(DataComponents.REPAIR_COST, 4);
            AnvilMenu h2 = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h2, sword2, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(h2, "X");
            h2.createResult();
            helper.assertFalse(h2.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "Vanilla rename must produce an output");
            helper.assertTrue(h2.getCost() == 5,
                "With prior_work_free off the penalty stays: 4+1=5 (got " + h2.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.succeed();
    }

    // not too expensive inclusive cap boundary

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void notTooExpensiveInclusiveBoundary(GameTestHelper helper) {
        ETTestHelper.setFeature("not_too_expensive", true);
        ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int cost;
        {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(200);
            sword.set(DataComponents.REPAIR_COST, 20);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.createResult();
            cost = h.getCost();
        }
        try {
            helper.assertTrue(cost > 0, "Baseline cost should be positive (got " + cost + ")");

            // cap == cost: inclusive >= must block (output empty)
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(cost));
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(200);
            sword.set(DataComponents.REPAIR_COST, 20);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.createResult();
            helper.assertTrue(h.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "cost==cap should block (>= is inclusive): cost=" + cost);

            // cap == cost + 1: must allow (output non-empty)
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(cost + 1));
            ItemStack sword2 = new ItemStack(Items.DIAMOND_SWORD);
            sword2.setDamageValue(200);
            sword2.set(DataComponents.REPAIR_COST, 20);
            AnvilMenu h2 = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h2, sword2, new ItemStack(Items.DIAMOND));
            h2.createResult();
            helper.assertFalse(h2.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "cost < cap (cap=cost+1) should allow: cost=" + cost);
        } finally {
            ETTestHelper.setFeature("not_too_expensive", false);
            ETTestHelper.setConfigValue("nte_max_cost", String.valueOf(Integer.MAX_VALUE));
        }
        helper.succeed();
    }

    // cheap names applies through take-output gameplay

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesRealTakeOutput(GameTestHelper helper) {
        ETTestHelper.setFeature("cheap_names", true);
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.giveExperienceLevels(5);
        try {
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.REPAIR_COST, 10);
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            boolean changed = handler.setItemName("Foo");
            handler.createResult(); // recompute explicitly after setting the new name
            helper.assertTrue(changed, "setNewItemName should report a change");
            helper.assertTrue(handler.getCost() == 1,
                "CheapNames pure rename should cost 1 (got " + handler.getCost() + ")");
            ItemStack out = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(out.isEmpty(), "Renamed output should be present");
            Component name = out.get(DataComponents.CUSTOM_NAME);
            helper.assertTrue(name != null && name.getString().equals("Foo"),
                "Output should be renamed to Foo (got " + (name == null ? "null" : name.getString()) + ")");
            helper.assertTrue(invokeCanTakeOutput(handler, player),
                "A 5-level player should be able to take a 1-level rename");
            ETTestHelper.invokeOnTakeOutput(handler, player, out);
            helper.assertTrue(player.experienceLevel == 4,
                "Taking a 1-level rename should leave the player at level 4 (got " + player.experienceLevel + ")");
            helper.assertTrue(handler.getSlot(AnvilMenu.INPUT_SLOT).getItem().isEmpty(),
                "Input slot 0 should be empty after taking the renamed item");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesVanillaCannotTake(GameTestHelper helper) {
        // without cheap names, this rename costs eleven levels
        // five levels cannot take the output
        ETTestHelper.setFeature("cheap_names", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.giveExperienceLevels(5);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.REPAIR_COST, 10);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(handler, "Foo");
            handler.createResult();
            helper.assertTrue(handler.getCost() == 11,
                "Vanilla rename with REPAIR_COST=10 should cost 11 (got " + handler.getCost() + ")");
            helper.assertFalse(invokeCanTakeOutput(handler, player),
                "A 5-level player must not be able to take an 11-level vanilla rename");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesClearNameRemovesCustomName(GameTestHelper helper) {
        // clearing a custom name still creates an operation
        // cheap names makes it cost one
        ETTestHelper.setFeature("cheap_names", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Old"));
            AnvilMenu handler = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            ETTestHelper.setAnvilNewName(handler, "");
            handler.createResult();
            helper.assertTrue(handler.getCost() == 1,
                "CheapNames should force a clear-name to cost 1 (got " + handler.getCost() + ")");
            ItemStack out = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(out.isEmpty(), "Clearing a name must still produce an output");
            helper.assertFalse(out.has(DataComponents.CUSTOM_NAME),
                "Clearing the name should leave the output without a CUSTOM_NAME");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cheapNamesSlot1EmptyNoRename(GameTestHelper helper) {
        // an empty second slot without renaming has no operation
        // cheap names preserves vanilla output and cost
        ETTestHelper.setFeature("cheap_names", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(handler, sword, ItemStack.EMPTY);
            handler.createResult();
            helper.assertTrue(handler.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "No rename and no material should produce an empty output");
            helper.assertTrue(handler.getCost() == 0,
                "CheapNames should retain vanilla zero cost when no operation exists (got " + handler.getCost() + ")");
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
        }
        helper.succeed();
    }

    // sturdy anvils damaged-anvil destroy branch

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void sturdyAnvilsDamagedDestroyed(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "1.0");
            ServerLevel world = helper.getLevel();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
            BlockPos absPos = helper.absolutePos(anvilPos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(10000);
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.createResult();
            ItemStack result = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(result.isEmpty(),
                "A damaged-anvil repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            helper.assertTrue(world.getBlockState(absPos).isAir(),
                "A DAMAGED_ANVIL with damage_chance=1.0 should be destroyed (air) on use");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // disabled enchantments are stripped during anvil merges
    // zero maximum level removes the enchantment

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsAnvilStrip(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("disable_enchantments_enabled",
            "disable_enchantments");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_SWORD),
                enchantedBook(Enchantments.SHARPNESS, 3));
            helper.assertTrue(out.isEmpty(), "A disabled enchantment merge should produce no output");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsAnvilStripDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("disable_enchantments_enabled", false);
        ETTestHelper.setConfigValue("disable_enchantments", "");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_SWORD),
                enchantedBook(Enchantments.SHARPNESS, 3));
            helper.assertFalse(out.isEmpty(), "A normal enchantment merge should produce an output");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out) == 3,
                "Sharpness should merge normally when not disabled (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out) + ")");
        } finally {
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        helper.succeed();
    }

    // god armor merges protection enchantments

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godArmorAnvilMerge(GameTestHelper helper) {
        ETTestHelper.setFeature("god_armor", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.enchant(Enchantments.PROTECTION, 4);
            ItemStack out = anvilMergeOutput(player, chest, enchantedBook(Enchantments.BLAST_PROTECTION, 4));
            helper.assertFalse(out.isEmpty(), "God Armor merge output should be present");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROTECTION, out) == 4,
                "Output should keep Protection IV (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROTECTION, out) + ")");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, out) == 4,
                "Output should gain Blast Protection IV (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, out) + ")");
        } finally {
            ETTestHelper.setFeature("god_armor", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godArmorAnvilMergeDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("god_armor", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.enchant(Enchantments.PROTECTION, 4);
            ItemStack out = anvilMergeOutput(player, chest, enchantedBook(Enchantments.BLAST_PROTECTION, 4));
            helper.assertTrue(out.isEmpty(),
                "Without God Armor, incompatible protection enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("god_armor", false);
        }
        helper.succeed();
    }

    // god weapons merges damage enchantments

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsAnvilMerge(GameTestHelper helper) {
        ETTestHelper.setFeature("god_weapons", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 3);
            ItemStack out = anvilMergeOutput(player, sword, enchantedBook(Enchantments.SMITE, 2));
            helper.assertFalse(out.isEmpty(), "God Weapons merge output should be present");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out) == 3,
                "Output should keep Sharpness III (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out) + ")");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, out) == 2,
                "Output should gain Smite II (got " + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, out)
                    + ")");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsAnvilMergeDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("god_weapons", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 3);
            ItemStack out = anvilMergeOutput(player, sword, enchantedBook(Enchantments.SMITE, 2));
            helper.assertTrue(out.isEmpty(),
                "Without God Weapons, incompatible damage enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsTridentCrossTweak(GameTestHelper helper) {
        // trident weapons allows sharpness on tridents
        // god weapons permits sharpness with impaling
        ETTestHelper.setFeature("god_weapons", true);
        ETTestHelper.setFeature("trident_weapons", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack trident = new ItemStack(Items.TRIDENT);
            trident.enchant(Enchantments.IMPALING, 3);
            ItemStack out = anvilMergeOutput(player, trident, enchantedBook(Enchantments.SHARPNESS, 2));
            helper.assertFalse(out.isEmpty(), "Cross-tweak merge output should be present");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.IMPALING, out) == 3,
                "Output should keep Impaling III (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.IMPALING, out) + ")");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out) == 2,
                "Output should gain Sharpness II (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out) + ")");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.succeed();
    }

    // multishot and piercing merge through anvils

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void multishotPiercingAnvilMerge(GameTestHelper helper) {
        ETTestHelper.setFeature("multishot_piercing", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.enchant(Enchantments.MULTISHOT, 1);
            ItemStack out = anvilMergeOutput(player, crossbow, enchantedBook(Enchantments.PIERCING, 1));
            helper.assertFalse(out.isEmpty(), "Multishot Piercing merge output should be present");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, out) == 1,
                "Output should keep Multishot I (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, out) + ")");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, out) == 1,
                "Output should gain Piercing I (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, out) + ")");
        } finally {
            ETTestHelper.setFeature("multishot_piercing", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void multishotPiercingAnvilMergeDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("multishot_piercing", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.enchant(Enchantments.MULTISHOT, 1);
            ItemStack out = anvilMergeOutput(player, crossbow, enchantedBook(Enchantments.PIERCING, 1));
            helper.assertTrue(out.isEmpty(),
                "Without Multishot Piercing, incompatible enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("multishot_piercing", false);
        }
        helper.succeed();
    }

    // trident weapons merge through anvils

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tridentWeaponsAnvilMerge(GameTestHelper helper) {
        ETTestHelper.setFeature("trident_weapons", true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack outFa = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.FIRE_ASPECT, 2));
            helper.assertFalse(outFa.isEmpty(), "Fire Aspect transfer should produce an output");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, outFa) == 2,
                "Fire Aspect II should transfer onto a trident (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, outFa) + ")");
            ItemStack outKb = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.KNOCKBACK, 2));
            helper.assertFalse(outKb.isEmpty(), "Knockback transfer should produce an output");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, outKb) == 2,
                "Knockback II should transfer onto a trident (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, outKb) + ")");
            ItemStack outLo = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.LOOTING, 3));
            helper.assertFalse(outLo.isEmpty(), "Looting transfer should produce an output");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOOTING, outLo) == 3,
                "Looting III should transfer onto a trident (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOOTING, outLo) + ")");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tridentWeaponsAnvilMergeDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("trident_weapons", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.TRIDENT),
                enchantedBook(Enchantments.FIRE_ASPECT, 2));
            helper.assertTrue(out.isEmpty(),
                "Without Trident Weapons, unsupported enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.succeed();
    }

    // axe weapons precedence with disabled enchantments

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axeWeaponsAnvilDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("axe_weapons", false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_AXE),
                enchantedBook(Enchantments.LOOTING, 3));
            helper.assertTrue(out.isEmpty(), "Without Axe Weapons, unsupported enchantments must not produce output");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axeWeaponsDisableEnchantPrecedence(GameTestHelper helper) {
        // disabled enchantments override axe weapons
        ETTestHelper.setFeature("axe_weapons", true);
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "looting");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack out = anvilMergeOutput(player, new ItemStack(Items.DIAMOND_AXE),
                enchantedBook(Enchantments.LOOTING, 3));
            helper.assertTrue(out.isEmpty(), "Disabled enchantments must prevent the axe merge output");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
            ETTestHelper.setConfigValue("disable_enchantments", "");
        }
        helper.succeed();
    }

    // raised caps apply during anvil merges

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void capmodAnvilClamp(GameTestHelper helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void capmodAnvilClampDisabled(GameTestHelper helper) {
        ETTestHelper.setCapmod(false);
        ETTestHelper.setEnchantCap("sharpness", -1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
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
        helper.succeed();
    }

    // edge cases

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperFractionalRounding(GameTestHelper helper) {
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "0.5");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            // repair cost three yields round two point five
            // half-up rounding produces three
            ItemStack sword3 = new ItemStack(Items.DIAMOND_SWORD);
            sword3.setDamageValue(200);
            sword3.set(DataComponents.REPAIR_COST, 3);
            ItemStack out3 = anvilMergeOutput(player, sword3, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out3.isEmpty(), "Fractional rounding repair should produce an output");
            int rc3 = out3.getOrDefault(DataComponents.REPAIR_COST, -1);
            helper.assertTrue(rc3 == 3, "round(0.5*3+1)=round(2.5) must be 3 (half-up), not 2 (got " + rc3 + ")");

            // round one point five yields two
            ItemStack sword1 = new ItemStack(Items.DIAMOND_SWORD);
            sword1.setDamageValue(200);
            sword1.set(DataComponents.REPAIR_COST, 1);
            ItemStack out1 = anvilMergeOutput(player, sword1, new ItemStack(Items.DIAMOND));
            helper.assertFalse(out1.isEmpty(), "Second fractional rounding repair should produce an output");
            int rc1 = out1.getOrDefault(DataComponents.REPAIR_COST, -1);
            helper.assertTrue(rc1 == 2, "round(0.5*1+1)=round(1.5) must be 2 (got " + rc1 + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkFreeCheapNamesCombineStillReduced(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ETTestHelper.setFeature("cheap_names", true);
        ETTestHelper.setFeature("prior_work_free", false);
        int fullCost;
        {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(500);
            sword.set(DataComponents.REPAIR_COST, 8);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.createResult();
            fullCost = h.getCost();
        }
        ETTestHelper.setFeature("prior_work_free", true);
        try {
            helper.assertTrue(fullCost > 8,
                "Baseline combine cost should exceed the 8-level prior-work penalty (got " + fullCost + ")");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(500);
            sword.set(DataComponents.REPAIR_COST, 8);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.createResult();
            helper.assertTrue(h.getCost() == fullCost - 8,
                "On a combine, PriorWorkFree must still subtract the penalty even with cheap_names on: expected "
                    + (fullCost - 8) + " got " + h.getCost());
        } finally {
            ETTestHelper.setFeature("cheap_names", false);
            ETTestHelper.setFeature("prior_work_free", false);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void priorWorkCheaperAndFreeCompose(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // baseline cost uses a repair without prior-work penalties
        ETTestHelper.setFeature("prior_work_free", false);
        ETTestHelper.setFeature("prior_work_cheaper", false);
        int baseCost;
        {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(200);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.createResult();
            baseCost = h.getCost();
        }
        ETTestHelper.setFeature("prior_work_free", true);
        ETTestHelper.setFeature("prior_work_cheaper", true);
        ETTestHelper.setConfigValue("pw_cost_multiplier", "1.0");
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(200);
            sword.set(DataComponents.REPAIR_COST, 4);
            AnvilMenu h = new AnvilMenu(0, player.getInventory());
            ETTestHelper.setAnvilInputs(h, sword, new ItemStack(Items.DIAMOND));
            h.createResult();
            // prior work free removes the four-level penalty
            // this matches the no-penalty baseline
            helper.assertTrue(h.getCost() == baseCost,
                "PriorWorkFree should remove the prior-work penalty (base=" + baseCost + " got=" + h.getCost() + ")");
            // prior work cheaper stamps repair cost five
            // vanilla would stamp nine
            helper.assertFalse(h.getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
                "PriorWorkCheaper repair should produce an output");
            int rc = h.getSlot(AnvilMenu.RESULT_SLOT).getItem().getOrDefault(DataComponents.REPAIR_COST, -1);
            helper.assertTrue(rc == 5, "PriorWorkCheaper should stamp REPAIR_COST=round(1.0*4+1)=5 (got " + rc + ")");
        } finally {
            ETTestHelper.setFeature("prior_work_free", false);
            ETTestHelper.setFeature("prior_work_cheaper", false);
            ETTestHelper.setConfigValue("pw_cost_multiplier", "1.33");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void sturdyAnvilsNegativeChanceClampsToZero(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "-5.0");
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
            ServerLevel world = helper.getLevel();
            BlockPos absPos = helper.absolutePos(anvilPos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(10000);
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            for (int i = 0; i < 10; i++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.setDamageValue(100);
                ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
                handler.createResult();
                ItemStack result = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                helper.assertFalse(result.isEmpty(),
                    "A negative-chance repair setup must produce an output before onTakeOutput");
                ETTestHelper.invokeOnTakeOutput(handler, player, result);
            }
            helper.assertBlockPresent(Blocks.ANVIL, anvilPos);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void sturdyAnvilsAboveOneClampsToOne(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "5.0");
            ServerLevel world = helper.getLevel();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
            BlockPos absPos = helper.absolutePos(anvilPos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(10000);
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.createResult();
            ItemStack result = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(result.isEmpty(),
                "An above-one-chance repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            helper.assertFalse(world.getBlockState(absPos).is(Blocks.ANVIL),
                "anvil_damage_chance=5.0 should clamp to 1.0 and degrade the anvil on first use");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void sturdyAnvilsCreativeNeverDamaged(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("sturdy_anvils", "anvil_damage_chance");
        try {
            ETTestHelper.setFeature("sturdy_anvils", true);
            ETTestHelper.setConfigValue("anvil_damage_chance", "1.0");
            ServerLevel world = helper.getLevel();
            BlockPos anvilPos = new BlockPos(0, 2, 0);
            helper.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());
            BlockPos absPos = helper.absolutePos(anvilPos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(10000);
            player.getAbilities().instabuild = true; // vanilla checks creative mode
            ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
            AnvilMenu handler = new AnvilMenu(0, player.getInventory(), ctx);
            helper.assertTrue(player.hasInfiniteMaterials(),
                "abilities.creativeMode must make isInCreativeMode() true (validates the setup)");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(100);
            ETTestHelper.setAnvilInputs(handler, sword, new ItemStack(Items.DIAMOND));
            handler.createResult();
            ItemStack result = handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(result.isEmpty(),
                "A creative-anvil repair setup must produce an output before onTakeOutput");
            ETTestHelper.invokeOnTakeOutput(handler, player, result);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.ANVIL),
                "A creative player must never damage the anvil, even at damage_chance=1.0");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairNegativeCostDisables(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "-5");
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        helper.setBlock(anvilPos.above(), Blocks.STONE.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);

            player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 9));
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Negative cost should disable ingot repair");
            helper.assertTrue(player.getMainHandItem().getCount() == 9,
                "No ingots should be consumed with a negative cost (got " + player.getMainHandItem().getCount() + ")");

            player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_BLOCK, 2));
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "Negative cost should disable block repair");
            helper.assertTrue(player.getMainHandItem().getCount() == 2,
                "No iron blocks should be consumed with a negative cost (got " + player.getMainHandItem().getCount()
                    + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void anvilRepairHugeCostUnaffordable(GameTestHelper helper) {
        ETTestHelper.setFeature("anvil_repair", true);
        ETTestHelper.setConfigValue("anvil_repair_ingot_cost", String.valueOf(Integer.MAX_VALUE));
        ServerLevel world = helper.getLevel();
        BlockPos anvilPos = new BlockPos(0, 2, 0);
        helper.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        helper.setBlock(anvilPos.above(), Blocks.STONE.defaultBlockState());
        BlockPos absPos = helper.absolutePos(anvilPos);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);

            player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_INGOT, 64));
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "A max-int cost must be unaffordable with ingots");
            helper.assertTrue(player.getMainHandItem().getCount() == 64,
                "No ingots consumed at an unaffordable cost (got " + player.getMainHandItem().getCount() + ")");

            player.getInventory().setItem(player.getInventory().selected, new ItemStack(Items.IRON_BLOCK, 64));
            player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(world.getBlockState(absPos).is(Blocks.DAMAGED_ANVIL),
                "A max-int cost (not a multiple of 9) must be unaffordable with blocks");
            helper.assertTrue(player.getMainHandItem().getCount() == 64,
                "No iron blocks consumed at an unaffordable cost (got " + player.getMainHandItem().getCount() + ")");
        } finally {
            ETTestHelper.setFeature("anvil_repair", false);
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "9");
        }
        helper.succeed();
    }

    // helpers

    /**
     * builds an anvil handler and updates its result returns the output stack
     */
    private static ItemStack anvilMergeOutput(Player player, ItemStack first, ItemStack second) {
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, first, second);
        handler.createResult();
        return handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
    }

    /** creates an enchanted book with one stored enchantment */
    private static ItemStack enchantedBook(Enchantment enchantment, int level) {
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
    }

    private static int bookOrItemLevel(Enchantment enchantment, ItemStack stack) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    /**
     * uses reflection to call protected can-take-output
     */
    private static boolean invokeCanTakeOutput(AnvilMenu handler, Player player) {
        try {
            Method m = AnvilMenu.class.getDeclaredMethod("canTakeOutput", Player.class, boolean.class);
            m.setAccessible(true);
            return (boolean) m.invoke(handler, player, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("canTakeOutput reflection failed", e);
        }
    }
}
