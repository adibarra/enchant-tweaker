package com.adibarra.enchanttweaker.test;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;

import com.adibarra.enchanttweaker.GrindstoneDisenchantAccess;

public class GrindstoneGameTest implements FabricGameTest {

    // gameplay helpers (private to this test; ETTestHelper is read-only)

    /**
     * the handler's shared input inventory, exposed by GrindstoneDisenchantMixin's
     * duck interface
     */
    private static Inventory grindstoneInput(GrindstoneScreenHandler handler) {
        return ((GrindstoneDisenchantAccess) handler).enchanttweaker$getInput();
    }

    private static void placeAndUpdate(GrindstoneScreenHandler handler, ItemStack slot0, ItemStack slot1) {
        Inventory input = grindstoneInput(handler);
        input.setStack(0, slot0);
        input.setStack(1, slot1);
        handler.onContentChanged(input);
    }

    /** reads the grindstone result through the real output slot (slot 2) */
    private static ItemStack resultStack(GrindstoneScreenHandler handler) {
        return handler.getSlot(2).getStack();
    }

    /** number of distinct enchantments (curses included) on a stack */
    private static int enchantCount(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).getEnchantments().size();
    }

    /** first stack of the given item found in the player's inventory, or EMPTY */
    private static ItemStack findInInventory(ServerPlayerEntity player, Item item) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(item))
                return s;
        }
        return ItemStack.EMPTY;
    }

    // grindstoneDisenchant: updateResult (real onContentChanged path)

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantToBook(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output");
            helper.assertTrue(result.isOf(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(result);
            helper.assertTrue(enchants.getLevel(Enchantments.SHARPNESS) == 3,
                "Output book should have Sharpness 3 (got " + enchants.getLevel(Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantCursesStay(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        sword.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output");
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(result);
            helper.assertTrue(enchants.getLevel(Enchantments.SHARPNESS) == 3, "Output book should have Sharpness 3");
            helper.assertTrue(enchants.getLevel(Enchantments.VANISHING_CURSE) == 0,
                "Output book should NOT have Vanishing Curse");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSplitBook(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        // a book with 2 enchantments: splitting extracts exactly one onto the output
        // book
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 3);
        enchantedBook.addEnchantment(Enchantments.UNBREAKING, 2);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output for book split");
            helper.assertTrue(result.isOf(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            helper.assertTrue(enchantCount(result) == 1,
                "Split book should have exactly 1 enchantment (got " + enchantCount(result) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSingleEnchantBook(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        // single-enchantment book: extract all (not split)
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 5);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Single-enchant book should still produce output");
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(result);
            helper.assertTrue(enchants.getLevel(Enchantments.SHARPNESS) == 5, "Output should have Sharpness 5");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantDisabled(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", false);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        placeAndUpdate(handler, book, sword);
        ItemStack result = resultStack(handler);

        // with the feature off, vanilla grindstone can't combine a book with a sword:
        // empty output
        helper.assertTrue(result.isEmpty(), "Grindstone disenchant disabled should not produce enchanted book output");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantCursesOnly(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        // item with only curses: no non-curse enchant to extract, so it falls through
        // to vanilla
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);

            helper.assertTrue(result.isEmpty(), "Curse-only item should not produce enchanted book output");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantNotADisenchantSetup(TestContext helper) {
        // `updateResult` bookSlot==-1 fall-through: feature ON, but neither slot
        // qualifies
        // (plain book + UNENCHANTED sword => no enchantments to extract => vanilla =>
        // empty)
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack plainSword = new ItemStack(Items.DIAMOND_SWORD); // no enchantments
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, plainSword);
            helper.assertTrue(resultStack(handler).isEmpty(),
                "Plain book + unenchanted item is not a disenchant setup: result must be empty");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneInputAcceptsPlainBookWhenEnabled(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        try {
            ItemStack book = new ItemStack(Items.BOOK);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack dirt = new ItemStack(Items.DIRT);

            // both input slots must accept a plain book when the feature is on
            helper.assertTrue(handler.getSlot(0).canInsert(book),
                "Input slot 0 should accept a plain book when grindstone_disenchant is on");
            helper.assertTrue(handler.getSlot(1).canInsert(book),
                "Input slot 1 should accept a plain book when grindstone_disenchant is on");

            // regression: a damageable item is still accepted (vanilla predicate untouched)
            // on BOTH slots
            helper.assertTrue(handler.getSlot(0).canInsert(sword),
                "Input slot 0 should still accept a damageable item");
            helper.assertTrue(handler.getSlot(1).canInsert(sword),
                "Input slot 1 ($3) should still accept a damageable item");

            // not broadened to junk: a non-book, non-damageable, unenchanted item is still
            // rejected on BOTH slots
            helper.assertFalse(handler.getSlot(0).canInsert(dirt),
                "Input slot 0 should still reject a non-book, non-damageable item");
            helper.assertFalse(handler.getSlot(1).canInsert(dirt),
                "Input slot 1 ($3) should still reject a non-book, non-damageable item");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneInputRejectsPlainBookWhenDisabled(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", false);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack book = new ItemStack(Items.BOOK);
        // feature off: vanilla predicate applies, so a plain book is rejected by both
        // input slots
        helper.assertFalse(handler.getSlot(0).canInsert(book),
            "Input slot 0 should reject a plain book when grindstone_disenchant is off");
        helper.assertFalse(handler.getSlot(1).canInsert(book),
            "Input slot 1 should reject a plain book when grindstone_disenchant is off");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantKeepItem(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            handler.getSlot(2).onTakeItem(player, result);

            helper.assertTrue(handler.getSlot(0).getStack().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getStack().isEmpty(), "Input slot 1 should be emptied after takeout");

            ItemStack returned = findInInventory(player, Items.DIAMOND_SWORD);
            helper.assertFalse(returned.isEmpty(), "keep_item=true should return the disenchanted sword to the player");
            helper.assertTrue(EnchantmentHelper.getEnchantments(returned).getLevel(Enchantments.SHARPNESS) == 0,
                "Returned sword should have Sharpness stripped (got "
                    + EnchantmentHelper.getEnchantments(returned).getLevel(Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantConsumeItem(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "false");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            handler.getSlot(2).onTakeItem(player, result);

            helper.assertTrue(handler.getSlot(0).getStack().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getStack().isEmpty(), "Input slot 1 should be emptied after takeout");

            helper.assertTrue(findInInventory(player, Items.DIAMOND_SWORD).isEmpty(),
                "keep_item=false should consume the input item (no sword returned to player)");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    // grindstoneOutputSlot: keep-branch variants additional cases

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSplitBookKeepBranch(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 3);
        enchantedBook.addEnchantment(Enchantments.UNBREAKING, 2);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Split should produce an output book");
            helper.assertTrue(result.isOf(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            helper.assertTrue(enchantCount(result) == 1,
                "Split output book should carry exactly 1 enchantment (got " + enchantCount(result) + ")");
            int outSharp = EnchantmentHelper.getEnchantments(result).getLevel(Enchantments.SHARPNESS);
            int outUnbreak = EnchantmentHelper.getEnchantments(result).getLevel(Enchantments.UNBREAKING);
            boolean extractedSharp = outSharp == 3 && outUnbreak == 0;
            boolean extractedUnbreak = outSharp == 0 && outUnbreak == 2;
            helper.assertTrue(extractedSharp || extractedUnbreak,
                "Output book must be exactly one of the source enchants at full level (Sharp=" + outSharp + ", Unbreak="
                    + outUnbreak + ")");

            handler.getSlot(2).onTakeItem(player, result);

            // both inputs consumed
            helper.assertTrue(handler.getSlot(0).getStack().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getStack().isEmpty(), "Input slot 1 should be emptied after takeout");

            // `keep_item=true:` the source book returns with the extracted enchant removed,
            // the other kept
            ItemStack kept = findInInventory(player, Items.ENCHANTED_BOOK);
            helper.assertFalse(kept.isEmpty(), "keep_item=true should return the leftover enchanted book");
            helper.assertTrue(enchantCount(kept) == 1,
                "Leftover book should keep exactly 1 enchantment (got " + enchantCount(kept) + ")");
            int keptSharp = EnchantmentHelper.getEnchantments(kept).getLevel(Enchantments.SHARPNESS);
            int keptUnbreak = EnchantmentHelper.getEnchantments(kept).getLevel(Enchantments.UNBREAKING);
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
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantKeepBranchRemainingEmpty(TestContext helper) {
        // book keep branch, remaining EMPTY => plain Items.BOOK returned
        // book(Sharpness5, single enchant) + plain book: all extracted, source has
        // nothing left
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 5);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Single-enchant book split should produce output");

            handler.getSlot(2).onTakeItem(player, result);

            helper.assertTrue(handler.getSlot(0).getStack().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getStack().isEmpty(), "Input slot 1 should be emptied after takeout");

            // remaining empty => the returned item degrades to a PLAIN book, not an
            // enchanted book
            ItemStack returnedPlain = findInInventory(player, Items.BOOK);
            helper.assertFalse(returnedPlain.isEmpty(),
                "keep_item=true with nothing remaining should return a plain Items.BOOK");
            helper.assertTrue(enchantCount(returnedPlain) == 0,
                "Returned plain book should carry no enchantments (got " + enchantCount(returnedPlain) + ")");
            helper.assertTrue(findInInventory(player, Items.ENCHANTED_BOOK).isEmpty(),
                "No enchanted book should be returned when the source book is fully drained");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantKeepBranchPreservesCurse(TestContext helper) {
        // regular-item keep branch preserves curses
        // sword(Sharpness3 + VanishingCurse1) + book: Sharpness goes to the output
        // book; taking it
        // returns the sword with Sharpness stripped but the curse retained
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        sword.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");
            helper.assertTrue(EnchantmentHelper.getEnchantments(result).getLevel(Enchantments.VANISHING_CURSE) == 0,
                "Output book must not carry the curse");

            handler.getSlot(2).onTakeItem(player, result);

            ItemStack returned = findInInventory(player, Items.DIAMOND_SWORD);
            helper.assertFalse(returned.isEmpty(), "keep_item=true should return the disenchanted sword");
            ItemEnchantmentsComponent kept = EnchantmentHelper.getEnchantments(returned);
            helper.assertTrue(kept.getLevel(Enchantments.SHARPNESS) == 0,
                "Returned sword should have Sharpness stripped (got " + kept.getLevel(Enchantments.SHARPNESS) + ")");
            helper.assertTrue(kept.getLevel(Enchantments.VANISHING_CURSE) == 1,
                "Returned sword should still carry Vanishing Curse (got " + kept.getLevel(Enchantments.VANISHING_CURSE)
                    + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSplitBookWithCurse(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 3);
        enchantedBook.addEnchantment(Enchantments.UNBREAKING, 2);
        enchantedBook.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Split-with-curse should produce an output book");
            helper.assertTrue(enchantCount(result) == 1,
                "Output book should carry exactly 1 (non-curse) enchant (got " + enchantCount(result) + ")");
            ItemEnchantmentsComponent out = EnchantmentHelper.getEnchantments(result);
            helper.assertTrue(out.getLevel(Enchantments.VANISHING_CURSE) == 0, "Output book must not carry the curse");
            int outSharp = out.getLevel(Enchantments.SHARPNESS);
            int outUnbreak = out.getLevel(Enchantments.UNBREAKING);
            boolean extractedSharp = outSharp == 3 && outUnbreak == 0;
            boolean extractedUnbreak = outSharp == 0 && outUnbreak == 2;
            helper.assertTrue(extractedSharp || extractedUnbreak,
                "Output book must be exactly one source enchant at full level (Sharp=" + outSharp + ", Unbreak="
                    + outUnbreak + ")");

            handler.getSlot(2).onTakeItem(player, result);

            ItemStack kept = findInInventory(player, Items.ENCHANTED_BOOK);
            helper.assertFalse(kept.isEmpty(), "Leftover enchanted book should be returned to the player");
            ItemEnchantmentsComponent keptE = EnchantmentHelper.getEnchantments(kept);
            helper.assertTrue(keptE.getLevel(Enchantments.VANISHING_CURSE) == 1,
                "Leftover book must retain the curse (got " + keptE.getLevel(Enchantments.VANISHING_CURSE) + ")");
            helper.assertTrue(enchantCount(kept) == 2,
                "Leftover book should keep the other enchant + the curse = 2 (got " + enchantCount(kept) + ")");
            if (extractedSharp) {
                helper.assertTrue(
                    keptE.getLevel(Enchantments.UNBREAKING) == 2 && keptE.getLevel(Enchantments.SHARPNESS) == 0,
                    "Extracted Sharpness => leftover keeps Unbreaking 2 + curse");
            } else {
                helper.assertTrue(
                    keptE.getLevel(Enchantments.SHARPNESS) == 3 && keptE.getLevel(Enchantments.UNBREAKING) == 0,
                    "Extracted Unbreaking => leftover keeps Sharpness 3 + curse");
            }
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantInsertFullDropsItem(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        // fill main inventory (slots 0..35) so insertStack of the returned sword fails
        for (int i = 0; i < 36; i++) {
            player.getInventory().setStack(i, new ItemStack(Items.STONE, 64));
        }

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
            player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            ServerWorld world = helper.getWorld();
            Box search = player.getBoundingBox().expand(8.0);
            // clear any pre-existing dropped swords in range to avoid stale matches
            world.getEntitiesByClass(ItemEntity.class, search, e -> e.getStack().isOf(Items.DIAMOND_SWORD))
                .forEach(ItemEntity::discard);

            handler.getSlot(2).onTakeItem(player, result);

            List<ItemEntity> dropped = world.getEntitiesByClass(ItemEntity.class, search,
                e -> e.getStack().isOf(Items.DIAMOND_SWORD));
            helper.assertFalse(dropped.isEmpty(),
                "Full inventory should force the stripped sword to drop as an ItemEntity");
            helper.assertTrue(handler.getSlot(0).getStack().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getStack().isEmpty(), "Input slot 1 should be emptied after takeout");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantTakeGatedWhenFeatureOff(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Grindstone should produce output book before takeout");

            // disable the feature: the take path must fall through to vanilla (no keep-item
            // logic)
            ETTestHelper.setFeature("grindstone_disenchant", false);
            handler.getSlot(2).onTakeItem(player, result); // must not throw

            helper.assertTrue(findInInventory(player, Items.DIAMOND_SWORD).isEmpty(),
                "Feature-off take should NOT return the stripped sword (mixin keep-logic gated off)");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    // edge sweep: extra updateResult / onTakeItem branches

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantMultiEnchantItemExtractsAll(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        sword.addEnchantment(Enchantments.LOOTING, 2);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Multi-enchant item should produce an output book");
            helper.assertTrue(result.isOf(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            ItemEnchantmentsComponent out = EnchantmentHelper.getEnchantments(result);
            helper.assertTrue(out.getLevel(Enchantments.SHARPNESS) == 3,
                "Output book should carry Sharpness 3 (got " + out.getLevel(Enchantments.SHARPNESS) + ")");
            helper.assertTrue(out.getLevel(Enchantments.LOOTING) == 2,
                "Output book should carry Looting 2 (got " + out.getLevel(Enchantments.LOOTING) + ")");
            helper.assertTrue(enchantCount(result) == 2,
                "Extract-all should place BOTH enchants on one book (got " + enchantCount(result) + ")");

            handler.getSlot(2).onTakeItem(player, result);

            ItemStack returned = findInInventory(player, Items.DIAMOND_SWORD);
            helper.assertFalse(returned.isEmpty(), "keep_item=true should return the stripped sword");
            ItemEnchantmentsComponent kept = EnchantmentHelper.getEnchantments(returned);
            helper.assertTrue(kept.getLevel(Enchantments.SHARPNESS) == 0 && kept.getLevel(Enchantments.LOOTING) == 0,
                "Returned sword should have BOTH non-curse enchants stripped (Sharp="
                    + kept.getLevel(Enchantments.SHARPNESS) + ", Loot=" + kept.getLevel(Enchantments.LOOTING) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSingleItemNoBookIsVanillaGrind(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);

        try {
            // enchanted sword in slot 0, slot 1 EMPTY
            placeAndUpdate(handler, sword, ItemStack.EMPTY);
            ItemStack result = resultStack(handler);

            helper.assertFalse(result.isEmpty(), "Vanilla single-item grind should still produce output");
            helper.assertFalse(result.isOf(Items.ENCHANTED_BOOK),
                "No book present => vanilla grind, NOT disenchant-to-book (result must not be an enchanted book)");
            helper.assertTrue(result.isOf(Items.DIAMOND_SWORD), "Ground result should still be the sword item");
            helper.assertTrue(EnchantmentHelper.getEnchantments(result).getLevel(Enchantments.SHARPNESS) == 0,
                "Vanilla grind should strip Sharpness off the ground sword (got "
                    + EnchantmentHelper.getEnchantments(result).getLevel(Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantBookStackDoesNotEngage(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack swordStack = new ItemStack(Items.DIAMOND_SWORD);
        swordStack.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack bookStack = new ItemStack(Items.BOOK, 2); // more than one book

        try {
            placeAndUpdate(handler, bookStack, swordStack);
            helper.assertTrue(resultStack(handler).isEmpty(),
                "A multi-count book stack must not engage disenchant (vanilla bails on count>1): result must be empty");

            // control: a single book still engages, proving the guard is specifically about
            // count>1
            ItemStack swordSingle = new ItemStack(Items.DIAMOND_SWORD);
            swordSingle.addEnchantment(Enchantments.SHARPNESS, 3);
            placeAndUpdate(handler, new ItemStack(Items.BOOK, 1), swordSingle);
            helper.assertFalse(resultStack(handler).isEmpty(),
                "A single book should still engage disenchant (count-specific guard control)");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSuppressesXpOnTakeout(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);

        ServerWorld world = helper.getWorld();
        // a real context tied to a loaded position inside the test structure; XP orbs
        // would spawn at
        // this block's center if vanilla onTakeItem ran
        BlockPos absPos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        ScreenHandlerContext ctx = ScreenHandlerContext.create(world, absPos);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory(), ctx);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, book, sword);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "Disenchant should produce an output book before takeout");

            Box search = new Box(absPos).expand(8.0);
            // clear any stale orbs so the post-takeout count reflects only this operation
            world.getEntitiesByClass(ExperienceOrbEntity.class, search, e -> true)
                .forEach(ExperienceOrbEntity::discard);

            handler.getSlot(2).onTakeItem(player, result);

            List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, search, e -> true);
            helper.assertTrue(orbs.isEmpty(),
                "Disenchant takeout must NOT spawn XP orbs (mixin suppresses vanilla grind XP); got " + orbs.size());
            // and the inputs are still emptied by the mixin's own path
            helper.assertTrue(handler.getSlot(0).getStack().isEmpty(), "Input slot 0 should be emptied after takeout");
            helper.assertTrue(handler.getSlot(1).getStack().isEmpty(), "Input slot 1 should be emptied after takeout");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSplitThreeEnchantLeavesTwo(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 3);
        enchantedBook.addEnchantment(Enchantments.UNBREAKING, 2);
        enchantedBook.addEnchantment(Enchantments.EFFICIENCY, 4);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            placeAndUpdate(handler, enchantedBook, book);
            ItemStack result = resultStack(handler);
            helper.assertFalse(result.isEmpty(), "3-enchant split should produce an output book");
            helper.assertTrue(enchantCount(result) == 1,
                "Split output book should carry exactly 1 enchant (got " + enchantCount(result) + ")");

            ItemEnchantmentsComponent out = EnchantmentHelper.getEnchantments(result);
            int outSharp = out.getLevel(Enchantments.SHARPNESS);
            int outUnbreak = out.getLevel(Enchantments.UNBREAKING);
            int outEff = out.getLevel(Enchantments.EFFICIENCY);

            handler.getSlot(2).onTakeItem(player, result);

            ItemStack kept = findInInventory(player, Items.ENCHANTED_BOOK);
            helper.assertFalse(kept.isEmpty(), "Leftover enchanted book should be returned");
            helper.assertTrue(enchantCount(kept) == 2,
                "Leftover book should keep exactly 2 enchants (got " + enchantCount(kept) + ")");
            ItemEnchantmentsComponent keptE = EnchantmentHelper.getEnchantments(kept);

            // complement invariant: each enchant lives entirely on exactly one of
            // output/kept
            helper.assertTrue(outSharp + keptE.getLevel(Enchantments.SHARPNESS) == 3,
                "Sharpness must total 3 across output+kept");
            helper.assertTrue(outUnbreak + keptE.getLevel(Enchantments.UNBREAKING) == 2,
                "Unbreaking must total 2 across output+kept");
            helper.assertTrue(outEff + keptE.getLevel(Enchantments.EFFICIENCY) == 4,
                "Efficiency must total 4 across output+kept");
            int extractedCount = (outSharp > 0 ? 1 : 0) + (outUnbreak > 0 ? 1 : 0) + (outEff > 0 ? 1 : 0);
            helper.assertTrue(extractedCount == 1,
                "Exactly one enchant should have been extracted onto the output book (got " + extractedCount + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
            ETTestHelper.setConfigValue("grindstone_disenchant_keep_item", "true");
        }
        helper.complete();
    }
}
