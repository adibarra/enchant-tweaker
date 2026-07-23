package com.adibarra.enchanttweaker.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.config.ETConfigScreenModel;

public class ConfigScreenModelGameTest implements FabricGameTest {

    private static final String EMPTY_STRUCTURE = "fabric-gametest-api-v1:empty";

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void screenModelPaginatesCategories(TestContext helper) {
        ETConfigScreenModel model = modelWithDefaults();
        helper.assertTrue(model.categories().equals(ETConfigSchema.categories()),
            "screen categories should preserve schema order");
        model.nextCategory();
        List<String> categoryKeys = ETConfigSchema.keysIn(model.currentCategory());
        helper.assertTrue(model.visibleKeys().equals(categoryKeys.subList(0, ETConfigScreenModel.PAGE_SIZE)),
            "first page should contain the first schema keys");

        model.nextPage();
        helper.assertTrue(model.pageIndex() == 1, "nextPage should advance within a multi-page category");
        model.nextCategory();
        helper.assertTrue(model.categoryIndex() == 2, "nextCategory should select the next category");
        helper.assertTrue(model.pageIndex() == 0, "changing category should reset pagination");
        model.previousCategory();
        helper.assertTrue(model.categoryIndex() == 1, "previousCategory should return to the prior category");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void screenModelValidatesAndTracksChanges(TestContext helper) {
        ETConfigScreenModel model = modelWithDefaults();
        helper.assertFalse(model.isDirty(), "fresh screen model should not be dirty");
        helper.assertFalse(model.setValue("nte_max_cost", "not-a-number"),
            "integer setting should reject non-numeric input");
        helper.assertFalse(model.isDirty(), "rejected input should not dirty the model");
        helper.assertTrue(model.setValue("nte_max_cost", "123"), "valid integer input should be accepted");
        helper.assertTrue("123".equals(model.value("nte_max_cost")), "accepted value should be retained");
        helper.assertTrue("123".equals(model.changes().get("nte_max_cost")),
            "accepted value should appear in the persistence delta");
        helper.assertFalse(model.hasRestartRequiredChanges(), "numeric-only changes should not require a restart");
        helper.assertTrue(model.setValue("cheap_names", "true"), "ordinary boolean input should be accepted");
        helper.assertFalse(model.hasRestartRequiredChanges(),
            "feature switches other than mod_enabled should reload without a restart");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void screenModelTogglesAndResetsCategory(TestContext helper) {
        Map<String, String> values = new LinkedHashMap<>(ETConfigSchema.defaults());
        values.put("mod_enabled", "true");
        ETConfigScreenModel model = new ETConfigScreenModel(values);

        helper.assertTrue(model.toggle("mod_enabled"), "boolean setting should toggle");
        helper.assertTrue("false".equals(model.value("mod_enabled")), "toggle should invert the boolean value");
        helper.assertTrue(model.hasRestartRequiredChanges(), "mod_enabled change should require a restart notice");
        helper.assertFalse(model.toggle("nte_max_cost"), "non-boolean setting should not toggle");

        model.resetCurrentCategory();
        helper.assertTrue(ETConfigSchema.defaultOf("mod_enabled").equals(model.value("mod_enabled")),
            "category reset should restore bundled defaults");
        helper.assertFalse(model.isDirty(), "resetting an otherwise-default category should clear its changes");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void screenModelCanonicalizesLoadedValues(TestContext helper) {
        Map<String, String> values = new LinkedHashMap<>(ETConfigSchema.defaults());
        values.put("mod_enabled", "TRUE");
        ETConfigScreenModel model = new ETConfigScreenModel(values);
        helper.assertTrue("true".equals(model.value("mod_enabled")),
            "loaded boolean values should be canonicalized to lowercase");

        helper.assertFalse(model.isDirty(), "canonicalized loaded values should start clean");
        helper.assertTrue(model.toggle("mod_enabled"), "first toggle should succeed");
        helper.assertTrue(model.toggle("mod_enabled"), "second toggle should succeed");
        helper.assertFalse(model.isDirty(), "boolean toggle round trip should restore the original value");
        helper.assertFalse(model.hasRestartRequiredChanges(), "boolean toggle round trip should not require a restart");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void screenModelSearchesKeysAndDescriptions(TestContext helper) {
        ETConfigScreenModel model = modelWithDefaults();
        model.nextCategory();
        int categoryIndex = model.categoryIndex();

        model.setSearchQuery("nte max");
        helper.assertTrue(model.hasSearchQuery(), "non-empty query should activate search mode");
        helper.assertTrue(model.visibleKeys().equals(List.of("nte_max_cost")),
            "search should match human-readable key words");
        model.nextCategory();
        helper.assertTrue(model.categoryIndex() == categoryIndex, "category navigation should pause during search");

        model.setSearchQuery("nineteen levels");
        helper.assertTrue(model.visibleKeys().equals(List.of("cheap_names")),
            "search should match bundled setting descriptions");
        model.setSearchQuery("no setting can possibly match this");
        helper.assertTrue(model.visibleKeys().isEmpty(), "unmatched search should return no settings");
        helper.assertTrue(model.pageCount() == 1, "empty search results should retain one display page");
        helper.complete();
    }

    private static ETConfigScreenModel modelWithDefaults() {
        return new ETConfigScreenModel(new LinkedHashMap<>(ETConfigSchema.defaults()));
    }
}
