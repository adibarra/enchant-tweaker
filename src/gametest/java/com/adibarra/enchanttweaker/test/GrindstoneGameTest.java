package com.adibarra.enchanttweaker.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import com.adibarra.enchanttweaker.GrindstoneDisenchantAccess;

public class GrindstoneGameTest {

    // test-local helpers use the read-only ettesthelper

    /**
     * exposes the handler's shared input inventory via the duck interface
     */
    private static Container grindstoneInput(GrindstoneMenu handler) {
        return ((GrindstoneDisenchantAccess) handler).enchanttweaker$getInput();
    }

    private static void placeAndUpdate(GrindstoneMenu handler, ItemStack slot0, ItemStack slot1) {
        Container input = grindstoneInput(handler);
        input.setItem(0, slot0);
        input.setItem(1, slot1);
        handler.slotsChanged(input);
    }

    /** reads the real result from output slot two */
    private static ItemStack resultStack(GrindstoneMenu handler) {
        return handler.getSlot(2).getItem();
    }

    /** counts distinct enchantments including curses */
    private static int enchantCount(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().size();
    }

    /** finds the first matching player inventory stack */
    private static ItemStack findInInventory(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item))
                return s;
        }
        return ItemStack.EMPTY;
    }

    // real grindstone content-change path

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantToBook(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output");
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
            helper.assertTrue(level(enchants, helper, Enchantments.SHARPNESS) == 3,
                "Output book should have Sharpness 3 (got " + level(enchants, helper, Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantCursesStay(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        sword.enchant(enchantment(helper, Enchantments.VANISHING_CURSE), 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output");
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
            helper.assertTrue(level(enchants, helper, Enchantments.SHARPNESS) == 3,
                "Output book should have Sharpness 3");
            helper.assertTrue(level(enchants, helper, Enchantments.VANISHING_CURSE) == 0,
                "Output book should NOT have Vanishing Curse");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantSplitBook(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        // splitting a two-enchantment book extracts one enchantment
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        enchantedBook.enchant(enchantment(helper, Enchantments.UNBREAKING), 2);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output for book split");
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            helper.assertTrue(enchantCount(result) == 1,
                "Split book should have exactly 1 enchantment (got " + enchantCount(result) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantSingleEnchantBook(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        // single-enchantment book: extract all (not split)
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.enchant(enchantment(helper, Enchantments.SHARPNESS), 5);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Single-enchant book should still produce output");
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
            helper.assertTrue(level(enchants, helper, Enchantments.SHARPNESS) == 5, "Output should have Sharpness 5");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", false);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        ItemStack book = new ItemStack(Items.BOOK);

        placeAndUpdate(handler, book, sword);
        ItemStack result = resultStack(handler);

        // vanilla cannot combine a plain book and sword
        helper.assertTrue(result.isEmpty(), "Grindstone disenchant disabled should not produce enchanted book output");
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantCursesOnly(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        // curse-only items have no enchantment to extract
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.VANISHING_CURSE), 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);

            helper.assertTrue(result.isEmpty(), "Curse-only item should not produce enchanted book output");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantNotADisenchantSetup(GameTestHelper helper) {
        // an unenchanted sword does not form a disenchant setup
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD); // no enchantments
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, plainSword);
            helper.assertTrue(resultStack(handler).isEmpty(),
                "Plain book + unenchanted item is not a disenchant setup: result must be empty");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneInputAcceptsPlainBookWhenEnabled(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        try {
            ItemStack book = new ItemStack(Items.BOOK);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack dirt = new ItemStack(Items.DIRT);

            // both input slots accept plain books when enabled
            helper.assertTrue(handler.getSlot(0).mayPlace(book),
                "Input slot 0 should accept a plain book when grindstone_disenchant is on");
            helper.assertTrue(handler.getSlot(1).mayPlace(book),
                "Input slot 1 should accept a plain book when grindstone_disenchant is on");
            helper.assertTrue(handler.getSlot(0).getMaxStackSize(book) == 1,
                "Input slot 0 should limit plain books to one");
            helper.assertTrue(handler.getSlot(1).getMaxStackSize(book) == 1,
                "Input slot 1 should limit plain books to one");

            // damageable items remain valid in both input slots
            helper.assertTrue(handler.getSlot(0).mayPlace(sword), "Input slot 0 should still accept a damageable item");
            helper.assertTrue(handler.getSlot(1).mayPlace(sword),
                "Input slot 1 ($3) should still accept a damageable item");

            // both slots reject unenchanted non-book nondamageable items
            helper.assertFalse(handler.getSlot(0).mayPlace(dirt),
                "Input slot 0 should still reject a non-book, non-damageable item");
            helper.assertFalse(handler.getSlot(1).mayPlace(dirt),
                "Input slot 1 ($3) should still reject a non-book, non-damageable item");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneInputRejectsPlainBookWhenDisabled(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", false);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack book = new ItemStack(Items.BOOK);
        // vanilla rejects plain books in both input slots
        helper.assertFalse(handler.getSlot(0).mayPlace(book),
            "Input slot 0 should reject a plain book when grindstone_disenchant is off");
        helper.assertFalse(handler.getSlot(1).mayPlace(book),
            "Input slot 1 should reject a plain book when grindstone_disenchant is off");
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantKeepItem(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        sword.set(DataComponents.REPAIR_COST, 7);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            handler.getSlot(2).onTake(player, result);

            helper.assertTrue(handler.getSlot(0).getItem().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getItem().isEmpty(), "Input slot 1 should be emptied after takeout");

            ItemStack returned = findInInventory(player, Items.DIAMOND_SWORD);
            helper.assertFalse(returned.isEmpty(), "keep_item=true should return the disenchanted sword to the player");
            ItemEnchantments returnedEnchants = EnchantmentHelper.getEnchantmentsForCrafting(returned);
            helper.assertTrue(level(returnedEnchants, helper, Enchantments.SHARPNESS) == 0,
                "Returned sword should have Sharpness stripped (got "
                    + level(returnedEnchants, helper, Enchantments.SHARPNESS) + ")");
            helper.assertTrue(returned.get(DataComponents.REPAIR_COST) == null,
                "Returned clean item should not retain prior-work cost");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantConsumeItem(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "false");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            handler.getSlot(2).onTake(player, result);

            helper.assertTrue(handler.getSlot(0).getItem().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getItem().isEmpty(), "Input slot 1 should be emptied after takeout");

            helper.assertTrue(findInInventory(player, Items.DIAMOND_SWORD).isEmpty(),
                "keep_item=false should consume the input item (no sword returned to player)");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    // additional keep-branch output cases

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantSplitBookKeepBranch(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        enchantedBook.enchant(enchantment(helper, Enchantments.UNBREAKING), 2);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Split should produce an output book");
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            helper.assertTrue(enchantCount(result) == 1,
                "Split output book should carry exactly 1 enchantment (got " + enchantCount(result) + ")");
            ItemEnchantments outEnchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
            int outSharp = level(outEnchants, helper, Enchantments.SHARPNESS);
            int outUnbreak = level(outEnchants, helper, Enchantments.UNBREAKING);
            boolean extractedSharp = outSharp == 3 && outUnbreak == 0;
            boolean extractedUnbreak = outSharp == 0 && outUnbreak == 2;
            helper.assertTrue(extractedSharp || extractedUnbreak,
                "Output book must be exactly one of the source enchants at full level (Sharp=" + outSharp + ", Unbreak="
                    + outUnbreak + ")");

            handler.getSlot(2).onTake(player, result);

            // taking output consumes both input stacks
            helper.assertTrue(handler.getSlot(0).getItem().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getItem().isEmpty(), "Input slot 1 should be emptied after takeout");

            // keeping source books removes only the extracted enchantment
            ItemStack kept = findInInventory(player, Items.ENCHANTED_BOOK);
            helper.assertFalse(kept.isEmpty(), "keep_item=true should return the leftover enchanted book");
            helper.assertTrue(enchantCount(kept) == 1,
                "Leftover book should keep exactly 1 enchantment (got " + enchantCount(kept) + ")");
            ItemEnchantments keptEnchants = EnchantmentHelper.getEnchantmentsForCrafting(kept);
            int keptSharp = level(keptEnchants, helper, Enchantments.SHARPNESS);
            int keptUnbreak = level(keptEnchants, helper, Enchantments.UNBREAKING);
            if (extractedSharp) {
                helper.assertTrue(keptSharp == 0 && keptUnbreak == 2,
                    "Extracted Sharpness => leftover keeps Unbreaking 2 only (Sharp=" + keptSharp + ", Unbreak="
                        + keptUnbreak + ")");
            } else {
                helper.assertTrue(keptSharp == 3 && keptUnbreak == 0,
                    "Extracted Unbreaking => leftover keeps Sharpness 3 only (Sharp=" + keptSharp + ", Unbreak="
                        + keptUnbreak + ")");
            }
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantKeepBranchRemainingEmpty(GameTestHelper helper) {
        // fully extracted books return as plain books
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.enchant(enchantment(helper, Enchantments.SHARPNESS), 5);
        enchantedBook.set(DataComponents.REPAIR_COST, 7);
        enchantedBook.set(DataComponents.STORED_ENCHANTMENTS,
            enchantedBook.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Single-enchant book split should produce output");

            handler.getSlot(2).onTake(player, result);

            helper.assertTrue(handler.getSlot(0).getItem().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getItem().isEmpty(), "Input slot 1 should be emptied after takeout");

            // fully drained books return as plain books
            ItemStack returnedPlain = findInInventory(player, Items.BOOK);
            helper.assertFalse(returnedPlain.isEmpty(),
                "keep_item=true with nothing remaining should return a plain Items.BOOK");
            helper.assertTrue(enchantCount(returnedPlain) == 0,
                "Returned plain book should carry no enchantments (got " + enchantCount(returnedPlain) + ")");
            helper.assertTrue(returnedPlain.get(DataComponents.STORED_ENCHANTMENTS) == null,
                "Returned plain book should not retain stored enchantments");
            helper.assertTrue(returnedPlain.get(DataComponents.REPAIR_COST) == null,
                "Returned plain book should not retain prior-work cost");
            helper.assertTrue(returnedPlain.get(DataComponents.ENCHANTMENTS) == null,
                "Returned plain book should use the canonical empty component set");
            helper.assertTrue(findInInventory(player, Items.ENCHANTED_BOOK).isEmpty(),
                "No enchanted book should be returned when the source book is fully drained");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantKeepBranchPreservesCurse(GameTestHelper helper) {
        // kept swords retain curses after extracting other enchantments
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        sword.enchant(enchantment(helper, Enchantments.VANISHING_CURSE), 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");
            helper.assertTrue(
                level(EnchantmentHelper.getEnchantmentsForCrafting(result), helper, Enchantments.VANISHING_CURSE) == 0,
                "Output book must not carry the curse");

            handler.getSlot(2).onTake(player, result);

            ItemStack returned = findInInventory(player, Items.DIAMOND_SWORD);
            helper.assertFalse(returned.isEmpty(), "keep_item=true should return the disenchanted sword");
            ItemEnchantments kept = EnchantmentHelper.getEnchantmentsForCrafting(returned);
            helper.assertTrue(level(kept, helper, Enchantments.SHARPNESS) == 0,
                "Returned sword should have Sharpness stripped (got "
                    + level(kept, helper, Enchantments.SHARPNESS) + ")");
            helper.assertTrue(level(kept, helper, Enchantments.VANISHING_CURSE) == 1,
                "Returned sword should still carry Vanishing Curse (got "
                    + level(kept, helper, Enchantments.VANISHING_CURSE) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantSplitBookWithCurse(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        enchantedBook.enchant(enchantment(helper, Enchantments.UNBREAKING), 2);
        enchantedBook.enchant(enchantment(helper, Enchantments.VANISHING_CURSE), 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Split-with-curse should produce an output book");
            helper.assertTrue(enchantCount(result) == 1,
                "Output book should carry exactly 1 (non-curse) enchant (got " + enchantCount(result) + ")");
            ItemEnchantments out = EnchantmentHelper.getEnchantmentsForCrafting(result);
            helper.assertTrue(level(out, helper, Enchantments.VANISHING_CURSE) == 0,
                "Output book must not carry the curse");
            int outSharp = level(out, helper, Enchantments.SHARPNESS);
            int outUnbreak = level(out, helper, Enchantments.UNBREAKING);
            boolean extractedSharp = outSharp == 3 && outUnbreak == 0;
            boolean extractedUnbreak = outSharp == 0 && outUnbreak == 2;
            helper.assertTrue(extractedSharp || extractedUnbreak,
                "Output book must be exactly one source enchant at full level (Sharp=" + outSharp + ", Unbreak="
                    + outUnbreak + ")");

            handler.getSlot(2).onTake(player, result);

            ItemStack kept = findInInventory(player, Items.ENCHANTED_BOOK);
            helper.assertFalse(kept.isEmpty(), "Leftover enchanted book should be returned to the player");
            ItemEnchantments keptE = EnchantmentHelper.getEnchantmentsForCrafting(kept);
            helper.assertTrue(level(keptE, helper, Enchantments.VANISHING_CURSE) == 1,
                "Leftover book must retain the curse (got " + level(keptE, helper, Enchantments.VANISHING_CURSE) + ")");
            helper.assertTrue(enchantCount(kept) == 2,
                "Leftover book should keep the other enchant + the curse = 2 (got " + enchantCount(kept) + ")");
            if (extractedSharp) {
                helper.assertTrue(
                    level(keptE, helper, Enchantments.UNBREAKING) == 2
                        && level(keptE, helper, Enchantments.SHARPNESS) == 0,
                    "Extracted Sharpness => leftover keeps Unbreaking 2 + curse");
            } else {
                helper.assertTrue(
                    level(keptE, helper, Enchantments.SHARPNESS) == 3
                        && level(keptE, helper, Enchantments.UNBREAKING) == 0,
                    "Extracted Unbreaking => leftover keeps Sharpness 3 + curse");
            }
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantInsertFullDropsItem(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        // fill inventory so returning the sword drops it
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            ServerLevel world = helper.getLevel();
            AABB search = player.getBoundingBox().inflate(8.0);
            // remove stale dropped swords before this assertion
            world.getEntitiesOfClass(ItemEntity.class, search, e -> e.getItem().is(Items.DIAMOND_SWORD))
                .forEach(ItemEntity::discard);

            handler.getSlot(2).onTake(player, result);

            List<ItemEntity> dropped = world.getEntitiesOfClass(ItemEntity.class, search,
                e -> e.getItem().is(Items.DIAMOND_SWORD));
            helper.assertFalse(dropped.isEmpty(),
                "Full inventory should force the stripped sword to drop as an ItemEntity");
            helper.assertTrue(handler.getSlot(0).getItem().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getItem().isEmpty(), "Input slot 1 should be emptied after takeout");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantTakeGatedWhenFeatureOff(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            // feature-off takeout uses vanilla behavior
            ETTestHelper.setFeature("grindstone_disenchant", false);
            handler.getSlot(2).onTake(player, result); // must not throw

            helper.assertTrue(findInInventory(player, Items.DIAMOND_SWORD).isEmpty(),
                "Feature-off take should NOT return the stripped sword (mixin keep-logic gated off)");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    // additional result and takeout branches

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantMultiEnchantItemExtractsAll(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        sword.enchant(enchantment(helper, Enchantments.LOOTING), 2);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Multi-enchant item should produce an output book");
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            ItemEnchantments out = EnchantmentHelper.getEnchantmentsForCrafting(result);
            helper.assertTrue(level(out, helper, Enchantments.SHARPNESS) == 3,
                "Output book should carry Sharpness 3 (got " + level(out, helper, Enchantments.SHARPNESS) + ")");
            helper.assertTrue(level(out, helper, Enchantments.LOOTING) == 2,
                "Output book should carry Looting 2 (got " + level(out, helper, Enchantments.LOOTING) + ")");
            helper.assertTrue(enchantCount(result) == 2,
                "Extract-all should place BOTH enchants on one book (got " + enchantCount(result) + ")");

            handler.getSlot(2).onTake(player, result);

            ItemStack returned = findInInventory(player, Items.DIAMOND_SWORD);
            helper.assertFalse(returned.isEmpty(), "keep_item=true should return the stripped sword");
            ItemEnchantments kept = EnchantmentHelper.getEnchantmentsForCrafting(returned);
            helper.assertTrue(
                level(kept, helper, Enchantments.SHARPNESS) == 0 && level(kept, helper, Enchantments.LOOTING) == 0,
                "Returned sword should have BOTH non-curse enchants stripped (Sharp="
                    + level(kept, helper, Enchantments.SHARPNESS) + ", Loot="
                    + level(kept, helper, Enchantments.LOOTING) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantSingleItemNoBookIsVanillaGrind(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);

        try {
            // a single enchanted sword uses vanilla grinding
            placeAndUpdate(handler, sword, ItemStack.EMPTY);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Vanilla single-item grind should still produce output");
            helper.assertFalse(result.is(Items.ENCHANTED_BOOK),
                "No book present => vanilla grind, NOT disenchant-to-book (result must not be an enchanted book)");
            helper.assertTrue(result.is(Items.DIAMOND_SWORD), "Ground result should still be the sword item");
            ItemEnchantments groundEnchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
            helper.assertTrue(level(groundEnchants, helper, Enchantments.SHARPNESS) == 0,
                "Vanilla grind should strip Sharpness off the ground sword (got "
                    + level(groundEnchants, helper, Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantBookStackDoesNotEngage(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack swordStack = new ItemStack(Items.DIAMOND_SWORD);
        swordStack.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        ItemStack bookStack = new ItemStack(Items.BOOK, 2); // more than one book

        try {
            // establish a valid output before transitioning to an invalid stack
            ItemStack validSword = new ItemStack(Items.DIAMOND_SWORD);
            validSword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
            placeAndUpdate(handler, new ItemStack(Items.BOOK, 1), validSword);
            helper.assertFalse(resultStack(handler).isEmpty(),
                "A single book should engage disenchant before the invalid transition");

            placeAndUpdate(handler, bookStack, swordStack);
            helper.assertTrue(resultStack(handler).isEmpty(),
                "A multi-count book stack must clear a previous disenchant output");

            // a single book confirms the count guard control
            ItemStack swordSingle = new ItemStack(Items.DIAMOND_SWORD);
            swordSingle.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
            placeAndUpdate(handler, new ItemStack(Items.BOOK, 1), swordSingle);
            helper.assertFalse(resultStack(handler).isEmpty(),
                "A single book should still engage disenchant after the invalid transition");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantConsumesOneBook(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "false");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        try {
            placeAndUpdate(handler, new ItemStack(Items.BOOK), sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Disenchant should produce an output book");
            handler.getSlot(0).getItem().grow(2);
            handler.getSlot(2).onTake(player, result);
            helper.assertTrue(handler.getSlot(0).getItem().getCount() == 2,
                "Taking one result should consume exactly one book");
            helper.assertTrue(handler.getSlot(1).getItem().isEmpty(),
                "Taking the result should consume the enchanted item");
        } finally {
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantSuppressesXpOnTakeout(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);

        ServerLevel world = helper.getLevel();
        // use a loaded context for vanilla takeout behavior
        BlockPos absPos = helper.absolutePos(new BlockPos(1, 2, 1));
        ContainerLevelAccess ctx = ContainerLevelAccess.create(world, absPos);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory(), ctx);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Disenchant should produce an output book before takeout");

            AABB search = new AABB(absPos).inflate(8.0);
            // remove stale experience orbs before takeout
            world.getEntitiesOfClass(ExperienceOrb.class, search, e -> true).forEach(ExperienceOrb::discard);

            handler.getSlot(2).onTake(player, result);

            List<ExperienceOrb> orbs = world.getEntitiesOfClass(ExperienceOrb.class, search, e -> true);
            helper.assertTrue(orbs.isEmpty(),
                "Disenchant takeout must NOT spawn XP orbs (mixin suppresses vanilla grind XP); got " + orbs.size());
            // the mixin still empties both inputs
            helper.assertTrue(handler.getSlot(0).getItem().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getItem().isEmpty(), "Input slot 1 should be emptied after takeout");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void grindstoneDisenchantSplitThreeEnchantLeavesTwo(GameTestHelper helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        GrindstoneMenu handler = new GrindstoneMenu(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.enchant(enchantment(helper, Enchantments.SHARPNESS), 3);
        enchantedBook.enchant(enchantment(helper, Enchantments.UNBREAKING), 2);
        enchantedBook.enchant(enchantment(helper, Enchantments.EFFICIENCY), 4);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "3-enchant split should produce an output book");
            helper.assertTrue(enchantCount(result) == 1,
                "Split output book should carry exactly 1 enchant (got " + enchantCount(result) + ")");

            ItemEnchantments out = EnchantmentHelper.getEnchantmentsForCrafting(result);
            int outSharp = level(out, helper, Enchantments.SHARPNESS);
            int outUnbreak = level(out, helper, Enchantments.UNBREAKING);
            int outEff = level(out, helper, Enchantments.EFFICIENCY);

            handler.getSlot(2).onTake(player, result);

            ItemStack kept = findInInventory(player, Items.ENCHANTED_BOOK);
            helper.assertFalse(kept.isEmpty(), "Leftover enchanted book should be returned");
            helper.assertTrue(enchantCount(kept) == 2,
                "Leftover book should keep exactly 2 enchants (got " + enchantCount(kept) + ")");
            ItemEnchantments keptE = EnchantmentHelper.getEnchantmentsForCrafting(kept);

            // each enchantment belongs entirely to output or kept stack
            helper.assertTrue(outSharp + level(keptE, helper, Enchantments.SHARPNESS) == 3,
                "Sharpness must total 3 across output+kept");
            helper.assertTrue(outUnbreak + level(keptE, helper, Enchantments.UNBREAKING) == 2,
                "Unbreaking must total 2 across output+kept");
            helper.assertTrue(outEff + level(keptE, helper, Enchantments.EFFICIENCY) == 4,
                "Efficiency must total 4 across output+kept");
            int extractedCount = (outSharp > 0 ? 1 : 0) + (outUnbreak > 0 ? 1 : 0) + (outEff > 0 ? 1 : 0);
            helper.assertTrue(extractedCount == 1,
                "Exactly one enchant should have been extracted onto the output book (got " + extractedCount + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.succeed();
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static int level(ItemEnchantments enchantments, GameTestHelper helper, ResourceKey<Enchantment> key) {
        return enchantments.getLevel(enchantment(helper, key));
    }
}
