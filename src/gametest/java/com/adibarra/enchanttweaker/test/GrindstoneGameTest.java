package com.adibarra.enchanttweaker.test;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;

public class GrindstoneGameTest implements FabricGameTest {

    // ─── GrindstoneDisenchant ──────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantToBook(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            ETTestHelper.setGrindstoneInputs(handler, book, sword);
            ETTestHelper.grindstoneUpdateResult(handler);
            ItemStack result = ETTestHelper.getGrindstoneResult(handler);

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

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantCursesStay(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        sword.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            ETTestHelper.setGrindstoneInputs(handler, book, sword);
            ETTestHelper.grindstoneUpdateResult(handler);
            ItemStack result = ETTestHelper.getGrindstoneResult(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output");
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(result);
            helper.assertTrue(enchants.getLevel(Enchantments.SHARPNESS) == 3,
                "Output book should have Sharpness 3");
            helper.assertTrue(enchants.getLevel(Enchantments.VANISHING_CURSE) == 0,
                "Output book should NOT have Vanishing Curse");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSplitBook(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        // Create an enchanted book with 2 enchantments
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 3);
        enchantedBook.addEnchantment(Enchantments.UNBREAKING, 2);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            ETTestHelper.setGrindstoneInputs(handler, enchantedBook, book);
            ETTestHelper.grindstoneUpdateResult(handler);
            ItemStack result = ETTestHelper.getGrindstoneResult(handler);

            helper.assertFalse(result.isEmpty(), "Grindstone should produce output for book split");
            helper.assertTrue(result.isOf(Items.ENCHANTED_BOOK), "Output should be an enchanted book");
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(result);

            // Splitting: should have exactly 1 enchantment (the first non-curse)
            int count = 0;
            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentsMap()) {
                count++;
            }
            helper.assertTrue(count == 1,
                "Split book should have exactly 1 enchantment (got " + count + ")");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantSingleEnchantBook(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        // Single-enchantment book: should extract all (not split)
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedBook.addEnchantment(Enchantments.SHARPNESS, 5);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            ETTestHelper.setGrindstoneInputs(handler, enchantedBook, book);
            ETTestHelper.grindstoneUpdateResult(handler);
            ItemStack result = ETTestHelper.getGrindstoneResult(handler);

            helper.assertFalse(result.isEmpty(), "Single-enchant book should still produce output");
            ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments(result);
            helper.assertTrue(enchants.getLevel(Enchantments.SHARPNESS) == 5,
                "Output should have Sharpness 5");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantDisabled(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", false);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        ItemStack book = new ItemStack(Items.BOOK);

        ETTestHelper.setGrindstoneInputs(handler, book, sword);
        ETTestHelper.grindstoneUpdateResult(handler);
        ItemStack result = ETTestHelper.getGrindstoneResult(handler);

        // When disabled, vanilla grindstone doesn't know what to do with book+sword, should be empty
        helper.assertTrue(result.isEmpty(),
            "Grindstone disenchant disabled should not produce enchanted book output");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void grindstoneDisenchantCursesOnly(TestContext helper) {
        ETTestHelper.setFeature("grindstone_disenchant", true);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        GrindstoneScreenHandler handler = new GrindstoneScreenHandler(0, player.getInventory());

        // Item with only curses: should fall through to vanilla
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        ItemStack book = new ItemStack(Items.BOOK);

        try {
            ETTestHelper.setGrindstoneInputs(handler, book, sword);
            ETTestHelper.grindstoneUpdateResult(handler);
            ItemStack result = ETTestHelper.getGrindstoneResult(handler);

            // Only curses: no non-curse to extract, should let vanilla handle (empty result)
            helper.assertTrue(result.isEmpty(),
                "Curse-only item should not produce enchanted book output");
        } finally {
            ETTestHelper.setFeature("grindstone_disenchant", false);
        }
        helper.complete();
    }
}
