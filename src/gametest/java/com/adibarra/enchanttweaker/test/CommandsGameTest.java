package com.adibarra.enchanttweaker.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import com.adibarra.enchanttweaker.AnvilRepairHandler;
import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.commands.suggestions.ValueSuggestion;
import com.adibarra.utils.ADBrigadier;

public class CommandsGameTest implements FabricGameTest {

    // /et config list

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void listOverviewExecutes(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        int result = dispatch(server, "et config list");
        helper.assertTrue(result >= 1, "config list overview should succeed");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void listCategoryExecutes(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        int result = dispatch(server, "et config list all");
        helper.assertTrue(result >= 1, "config list all should succeed");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void listInvalidCategoryFails(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        int result = dispatch(server, "et config list notacategory");
        helper.assertTrue(result == 0, "unknown category should fail (return 0)");
        helper.complete();
    }

    // named categories and pages share this command path
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void listNamedCategoryAndPage(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        helper.assertTrue(dispatch(server, "et config list anvil_tweaks") >= 1,
            "listing a named category should succeed");
        // more than fifteen keys create a second all page
        helper.assertTrue(dispatch(server, "et config list all 2") >= 1, "listing page 2 of all keys should succeed");
        helper.complete();
    }

    // out-of-range pages follow the error path
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void listOutOfRangePageFails(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        helper.assertTrue(dispatch(server, "et config list all 9999") == 0,
            "an out-of-range page should fail (return 0)");
        helper.complete();
    }

    // disabled enchantments default to an empty value
    // it appears on other tweaks page one
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void listRendersEmptyValues(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        CapturingOutput out = new CapturingOutput();
        int result = dispatchCapturing(server, "et config list other_tweaks", out);
        helper.assertTrue(result >= 1, "listing other_tweaks should succeed");
        helper.assertTrue(out.text().contains("(empty)"),
            "an empty-valued key (disable_enchantments) should render as (empty) (got: " + out.text() + ")");
        helper.complete();
    }

    // et config key get command

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void getCommandThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        helper.assertTrue(dispatch(server, "et config cheap_names") >= 1, "GET of an existing key should succeed");
        helper.assertTrue(dispatch(server, "et config does_not_exist") == 0,
            "GET of a nonexistent key should fail (return 0)");
        helper.complete();
    }

    // reserved keys allow get but reject set and reset
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void reservedKeyGetAllowed(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        helper.assertTrue(dispatch(server, "et config config_version") >= 1,
            "GET of a reserved key should be allowed even though SET/RESET are rejected");
        helper.complete();
    }

    // /et reload
    // reload rebuilds configuration from disk
    // in-memory changes are discarded

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void reloadRereadsDisk(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names");
        try {
            // set config value only mutates memory
            // reload overwrites it from disk
            ETTestHelper.setConfigValue("cheap_names", "true");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "precondition: cheap_names should be true in memory before reload");
            int result = dispatch(server, "et reload");
            helper.assertTrue(result >= 1, "reload should succeed");
            helper.assertTrue(
                Objects.equals(originalConfig.get("cheap_names"),
                    ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)),
                "reload should restore cheap_names to its on-disk value");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // /et config reset

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void resetSingleKeyRestoresDefault(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names");
        try {
            ETTestHelper.setFeature("cheap_names", true);
            int result = dispatch(server, "et config reset cheap_names");
            helper.assertTrue(result >= 1, "reset cheap_names should succeed");
            helper.assertTrue(!ETMixinPlugin.getConfig().getOrDefault("cheap_names", true),
                "cheap_names should be restored to its default (false)");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void resetAllRestoresDefaults(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        String[] keys = ETMixinPlugin.getConfig().getKeys().toArray(new String[0]);
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig(keys);
        try {
            ETTestHelper.setFeature("cheap_names", true);
            ETTestHelper.setFeature("god_armor", true);
            ETTestHelper.setConfigValue("nte_max_cost", "100");
            int result = dispatch(server, "et config reset all");
            helper.assertTrue(result >= 1, "reset all should succeed");
            helper.assertTrue(!ETMixinPlugin.getConfig().getOrDefault("cheap_names", true),
                "cheap_names should be restored to default");
            helper.assertTrue(!ETMixinPlugin.getConfig().getOrDefault("god_armor", true),
                "god_armor should be restored to default");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", -1) == 2147483647,
                "nte_max_cost should be restored to default");
            helper.assertTrue("1".equals(ETMixinPlugin.getConfig().getOrDefault("config_version", "?")),
                "config_version should remain 1 after reset all");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    // bare resets print usage and succeed
    // unknown keys fail
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void resetBareAndNonexistent(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        helper.assertTrue(dispatch(server, "et config reset") == 1,
            "bare reset should print usage and return SINGLE_SUCCESS");
        helper.assertTrue(dispatch(server, "et config reset not_a_real_key") == 0,
            "resetting a nonexistent key should fail (return 0)");
        helper.complete();
    }

    // reset all writes every default in one update
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void resetAllPersistsDefaultsToDisk(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        String path = ETMixinPlugin.getConfig().getConfigPath();
        String[] keys = ETMixinPlugin.getConfig().getKeys().toArray(new String[0]);
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig(keys);
        helper.assertTrue(path != null, "config path should be resolvable");
        try {
            helper.assertTrue(dispatch(server, "et config nte_max_cost 500") >= 1,
                "set nte_max_cost 500 should succeed");
            helper.assertTrue(dispatch(server, "et config reset all") >= 1, "reset all should succeed");
            String fileText = Files.readString(Path.of(path));
            helper.assertTrue(fileText.contains("nte_max_cost=2147483647"),
                "reset all should persist the bundled default nte_max_cost=2147483647 to disk");
            helper.assertTrue(!fileText.contains("nte_max_cost=500"),
                "the transient value 500 must not remain in the file after reset all");
            helper.assertTrue(fileText.contains("config_version=1"),
                "config_version=1 (reserved) should survive reset all");
        } catch (java.io.IOException e) {
            throw new RuntimeException("failed to read config file", e);
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void configCommandsRejectPersistenceFailure(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        String path = ETMixinPlugin.getConfig().getConfigPath();
        helper.assertTrue(path != null, "config path should be resolvable");
        Path configPath = Path.of(path);
        Path backup = null;
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names");
        try {
            backup = Files.createTempFile(configPath.getParent(), "et-persist-failure-", ".properties");
            Files.delete(backup);
            Files.move(configPath, backup);
            Files.createDirectory(configPath);

            CapturingOutput setOutput = new CapturingOutput();
            helper.assertTrue(dispatchCapturing(server, "et config cheap_names true", setOutput) == 0,
                "set must fail when the config file cannot be read");
            helper.assertTrue(setOutput.text().contains("Failed to persist key"),
                "set should report the persistence failure");
            helper.assertTrue(
                Objects.equals(originalConfig.get("cheap_names"),
                    ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)),
                "failed set must leave memory unchanged");

            CapturingOutput resetOutput = new CapturingOutput();
            helper.assertTrue(dispatchCapturing(server, "et config reset cheap_names", resetOutput) == 0,
                "single-key reset must fail when the config file cannot be read");
            helper.assertTrue(resetOutput.text().contains("Failed to persist reset"),
                "single-key reset should report the persistence failure");
            helper.assertTrue(
                Objects.equals(originalConfig.get("cheap_names"),
                    ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)),
                "failed single-key reset must leave memory unchanged");

            CapturingOutput resetAllOutput = new CapturingOutput();
            helper.assertTrue(dispatchCapturing(server, "et config reset all", resetAllOutput) == 0,
                "reset all must fail when the config file cannot be read");
            helper.assertTrue(resetAllOutput.text().contains("Failed to persist config reset"),
                "reset all should report the persistence failure");
            helper.assertTrue(
                Objects.equals(originalConfig.get("cheap_names"),
                    ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)),
                "failed reset all must leave memory unchanged");
        } catch (java.io.IOException e) {
            throw new RuntimeException("config persistence failure setup failed", e);
        } finally {
            try {
                if (Files.isDirectory(configPath))
                    Files.delete(configPath);
                if (backup != null && Files.exists(backup))
                    Files.move(backup, configPath);
            } catch (java.io.IOException e) {
                throw new RuntimeException("config persistence failure cleanup failed", e);
            }
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // base et commands require permission level two

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void permissionLevelTwoGate(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ServerCommandSource playerSource = player.getCommandSource();

        // mock players have permission level zero
        helper.assertTrue(!playerSource.hasPermissionLevel(2), "mock player should be below permission level 2");

        // the base command is hidden from low-permission sources
        // execution then throws a syntax exception
        boolean denied = false;
        try {
            server.getCommandManager().getDispatcher().execute("et config list", playerSource);
        } catch (CommandSyntaxException e) {
            denied = true;
        }
        helper.assertTrue(denied, "a source below level 2 should be denied (dispatch throws)");

        // contrast: the level-4 server source succeeds on the same command
        helper.assertTrue(dispatch(server, "et config list") >= 1, "the level-4 server source should be allowed");
        helper.complete();
    }

    // et config key value boolean normalization

    // greedy strings accept boolean aliases
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setBoolAliasesThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names");
        try {
            helper.assertTrue(dispatch(server, "et config cheap_names YeS") >= 1, "'YeS' should be accepted");
            helper.assertTrue("true".equals(ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)),
                "'YeS' should normalize to 'true' (got " + ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)
                    + ")");

            helper.assertTrue(dispatch(server, "et config cheap_names oFf") >= 1, "'oFf' should be accepted");
            helper.assertTrue("false".equals(ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)),
                "'oFf' should normalize to 'false' (got " + ETMixinPlugin.getConfig().getOrDefault("cheap_names", null)
                    + ")");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setInvalidValueRejected(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        // nte max cost is an integer
        // banana fails schema validation
        String before = ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", null);
        int result = dispatch(server, "et config nte_max_cost banana");
        String after = ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", null);
        helper.assertTrue(result == 0, "invalid value should fail (return 0)");
        helper.assertTrue(java.util.Objects.equals(before, after),
            "nte_max_cost must be unchanged after an invalid set (before=" + before + " after=" + after + ")");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setValidValueThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("nte_max_cost");
        try {
            int result = dispatch(server, "et config nte_max_cost 500");
            helper.assertTrue(result >= 1, "valid set should succeed");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", -1) == 500,
                "nte_max_cost should be updated to 500 (got "
                    + ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", -1) + ")");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setBooleanThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names");
        try {
            // single-token greedy strings still normalize and validate
            int result = dispatch(server, "et config cheap_names true");
            helper.assertTrue(result >= 1, "boolean set should succeed");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "cheap_names should be true after set");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setCommaListThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("protection_bypass_types");
        try {
            int result = dispatch(server, "et config protection_bypass_types magic,wither");
            helper.assertTrue(result >= 1, "comma-separated list set should succeed");
            String stored = ETMixinPlugin.getConfig().getOrDefault("protection_bypass_types", null);
            helper.assertTrue("magic,wither".equals(stored),
                "protection_bypass_types should be 'magic,wither' (got '" + stored + "')");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    // schema rejection through command dispatch

    // overflow and decimal integers are rejected
    // values remain unchanged
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void integerSchemaRejectionsThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        assertRejectedUnchanged(helper, server, "nte_max_cost", "2147483648"); // above integer maximum
        assertRejectedUnchanged(helper, server, "nte_max_cost", "1.5"); // decimal for an int key
        helper.complete();
    }

    // non-finite decimal values are rejected
    // values remain unchanged
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void decimalSchemaRejectionsThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        assertRejectedUnchanged(helper, server, "anvil_damage_chance", "nan");
        assertRejectedUnchanged(helper, server, "anvil_damage_chance", "infinity");
        helper.complete();
    }

    // non-boolean values leave configuration unchanged
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void booleanSchemaRejectionThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        assertRejectedUnchanged(helper, server, "cheap_names", "banana");
        helper.complete();
    }

    // reserved keys reject set and reset

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void resetReservedRejected(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        String before = ETMixinPlugin.getConfig().getOrDefault("config_version", null);
        int result = dispatch(server, "et config reset config_version");
        String after = ETMixinPlugin.getConfig().getOrDefault("config_version", null);
        helper.assertTrue(result == 0, "resetting a reserved key should fail (return 0)");
        helper.assertTrue(java.util.Objects.equals(before, after), "config_version must be unchanged");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setReservedRejected(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        String before = ETMixinPlugin.getConfig().getOrDefault("config_version", null);
        int result = dispatch(server, "et config config_version 5");
        String after = ETMixinPlugin.getConfig().getOrDefault("config_version", null);
        helper.assertTrue(result == 0, "setting a reserved key should fail (return 0)");
        helper.assertTrue(java.util.Objects.equals(before, after), "config_version must be unchanged");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void modEnabledToggleThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("mod_enabled", "capmod_enabled");
        // capmod is enabled, leaving mod enabled as gate
        ETTestHelper.setCapmod(true);
        CapturingOutput out = new CapturingOutput();
        try {
            int off = dispatchCapturing(server, "et config mod_enabled false", out);
            helper.assertTrue(off >= 1, "setting mod_enabled=false should succeed");
            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("mod_enabled", true),
                "mod_enabled should be false in memory after the set");
            helper.assertFalse(ETMixinPlugin.getMixinConfig("CapmodMixin"),
                "CapmodMixin must be gated off when mod_enabled=false");
            helper.assertTrue(out.text().contains("restart to take effect"),
                "SET mod_enabled should emit the restart note (got: " + out.text() + ")");

            out.messages.clear();
            int on = dispatchCapturing(server, "et config reset mod_enabled", out);
            helper.assertTrue(on >= 1, "resetting mod_enabled should succeed");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("mod_enabled", false),
                "mod_enabled should be true again after reset");
            helper.assertTrue(ETMixinPlugin.getMixinConfig("CapmodMixin"),
                "the CapmodMixin gate should lift once mod_enabled is true again (capmod still on)");
            helper.assertTrue(out.text().contains("restart to take effect"),
                "RESET mod_enabled should emit the restart note (got: " + out.text() + ")");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    // value suggestions

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void valueSuggestionCandidates(TestContext helper) {
        DynamicRegistryManager rm = helper.getWorld().getRegistryManager();

        // booleans suggest true and false
        List<String> bools = ValueSuggestion.candidatesFor("cheap_names", rm);
        helper.assertTrue(bools.contains("true") && bools.contains("false") && bools.size() == 2,
            "boolean candidates should be exactly [true, false]");

        // integers suggest current and default values
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("nte_max_cost");
        try {
            ETTestHelper.setConfigValue("nte_max_cost", "100");
            List<String> ints = ValueSuggestion.candidatesFor("nte_max_cost", rm);
            helper.assertTrue(ints.contains("100"), "integer candidates should contain the current value");
            helper.assertTrue(ints.contains("2147483647"), "integer candidates should contain the bundled default");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        // enchantments suggest registry paths
        List<String> enchants = ValueSuggestion.candidatesFor("disable_enchantments", rm);
        helper.assertTrue(enchants.contains("sharpness"), "enchantment candidates should contain 'sharpness'");
        List<String> expectedEnchantments = rm.get(RegistryKeys.ENCHANTMENT).getIds().stream()
            .map(id -> Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace()) ? id.getPath() : id.toString()).sorted()
            .toList();
        helper.assertTrue(enchants.equals(expectedEnchantments),
            "enchantment candidates should mirror the active dynamic registry");

        // vanilla damage types suggest bare registry paths
        List<String> damageTypes = ValueSuggestion.candidatesFor("protection_bypass_types", rm);
        helper.assertTrue(damageTypes.contains("arrow"),
            "damage-type candidates should contain the bare vanilla path 'arrow'");

        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void valueSuggestionSegmentCompletionThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();

        // prefixes complete the only list segment
        List<String> first = completions(server, "et config disable_enchantments sharp");
        helper.assertTrue(first.contains("sharpness"), "'sharp' should suggest 'sharpness' (got " + first + ")");

        List<String> segment = completions(server, "et config disable_enchantments sharpness,mend");
        helper.assertTrue(segment.contains("sharpness,mending"),
            "'sharpness,mend' should suggest 'sharpness,mending' (got " + segment + ")");

        helper.complete();
    }

    // test the provider through dispatcher completions
    // cover values plus reserved and unknown keys
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void valueSuggestionProviderThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();

        // empty boolean queries suggest both values
        List<String> bools = completions(server, "et config cheap_names ");
        helper.assertTrue(bools.contains("true") && bools.contains("false"),
            "boolean value completion should offer true and false (got " + bools + ")");

        // decimal values suggest current and default values
        List<String> dec = completions(server, "et config pw_cost_multiplier ");
        helper.assertTrue(dec.contains("1.33"),
            "decimal value completion should offer the current/default 1.33 (got " + dec + ")");

        // integer values suggest current and default values
        List<String> ints = completions(server, "et config nte_max_cost ");
        helper.assertTrue(ints.contains("2147483647"),
            "integer value completion should offer the bundled default (got " + ints + ")");

        // reserved keys have no candidates
        List<String> reserved = completions(server, "et config config_version ");
        helper.assertTrue(reserved.isEmpty(),
            "a reserved key should offer no value completions (got " + reserved + ")");

        // unknown keys have no candidates
        List<String> unknown = completions(server, "et config not_a_real_key ");
        helper.assertTrue(unknown.isEmpty(), "an unknown key should offer no value completions (got " + unknown + ")");

        helper.complete();
    }

    // enchanttweaker alias node

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void enchanttweakerAliasDispatches(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        helper.assertTrue(dispatch(server, "enchanttweaker config list") >= 1,
            "the enchanttweaker alias should route to the same command tree");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void aliasCopiesChildrenAndRedirects(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        CommandDispatcher<ServerCommandSource> dispatcher = new CommandDispatcher<>();
        LiteralCommandNode<ServerCommandSource> target = CommandManager.literal("target").executes(context -> 7)
            .build();
        LiteralCommandNode<ServerCommandSource> destination = CommandManager.literal("source")
            .then(CommandManager.literal("nested").then(CommandManager.literal("leaf").executes(context -> 11)))
            .build();
        LiteralCommandNode<ServerCommandSource> alias = ADBrigadier.buildAlias("alias", destination);
        LiteralCommandNode<ServerCommandSource> redirected = CommandManager.literal("redirected").redirect(target)
            .build();
        LiteralCommandNode<ServerCommandSource> redirectAlias = ADBrigadier.buildAlias("redirect_alias", redirected);

        dispatcher.getRoot().addChild(target);
        dispatcher.getRoot().addChild(destination);
        dispatcher.getRoot().addChild(alias);
        dispatcher.getRoot().addChild(redirectAlias);

        helper.assertTrue(alias.getChild("nested") != destination.getChild("nested"),
            "alias children should be copied instead of reused");
        try {
            helper.assertTrue(dispatcher.execute("alias nested leaf", server.getCommandSource()) == 11,
                "an aliased nested path should execute");
            helper.assertTrue(redirectAlias.getRedirect() == target, "a redirected alias should retain its target");
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("alias dispatch failed", e);
        }
        helper.complete();
    }

    // trivial no-argument executors

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void trivialExecutorsSucceed(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        helper.assertTrue(dispatch(server, "et") == 1, "'et' base command should return SINGLE_SUCCESS");
        helper.assertTrue(dispatch(server, "et config") == 1, "'et config' usage should return SINGLE_SUCCESS");
        helper.assertTrue(dispatch(server, "et help") == 1, "'et help' should return SINGLE_SUCCESS");
        helper.complete();
    }

    // et diagnose and accessors

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void diagnoseAccessorsAndCommand(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();

        Map<String, String> mixinKeys = ETMixinPlugin.getMixinKeys();
        helper.assertTrue(!mixinKeys.isEmpty(), "getMixinKeys() should be non-empty");
        helper.assertTrue(ETMixinPlugin.getActiveCompatOverrides().isEmpty(),
            "no compat overrides should be active in the test env");
        helper.assertTrue(AnvilRepairHandler.isRegistered(), "anvil repair handler should be registered");

        int result = dispatch(server, "et diagnose");
        helper.assertTrue(result >= 1, "et diagnose should succeed");
        helper.complete();
    }

    // test a feature enabled while its mixin is gated
    // mod enabled false creates the desynchronization
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void diagnoseReportsModDisabledDesync(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names", "mod_enabled");
        ETTestHelper.setFeature("cheap_names", true);
        // memory-only mod disabled clears cached mixin configuration
        // the next lookup recomputes configuration
        ETTestHelper.setConfigValue("mod_enabled", "false");
        CapturingOutput out = new CapturingOutput();
        try {
            int result = dispatchCapturing(server, "et diagnose", out);
            helper.assertTrue(result >= 1, "diagnose should succeed");
            helper.assertTrue(out.text().contains("cheap_names - mod_enabled is currently false"),
                "diagnose should flag cheap_names as desynced because mod_enabled=false (got: " + out.text() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void diagnoseReportsStaleEnabledFeature(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("mod_enabled", "cheap_names");
        try {
            ETTestHelper.setConfigValue("mod_enabled", "true");
            ETTestHelper.setFeature("cheap_names", true);
            helper.assertTrue(ETMixinPlugin.getMixinConfig("CheapNamesMixin"),
                "precondition: cheap_names should be cached as active");
            ETMixinPlugin.getConfig().setAll(Map.of("cheap_names", "false"));

            CapturingOutput out = new CapturingOutput();
            dispatchCapturing(server, "et diagnose", out);
            helper.assertTrue(out.text().contains("cheap_names - feature remains active while configured false"),
                "diagnose should report stale active features (got: " + out.text() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // diagnose distinguishes disabled and non-block-payable repair costs
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void diagnoseAnvilCostBranches(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("anvil_repair_ingot_cost");
        try {
            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "0");
            CapturingOutput disabled = new CapturingOutput();
            dispatchCapturing(server, "et diagnose", disabled);
            helper.assertTrue(disabled.text().contains("(repair disabled)"),
                "cost 0 should report '(repair disabled)' (got: " + disabled.text() + ")");

            ETTestHelper.setConfigValue("anvil_repair_ingot_cost", "5");
            CapturingOutput notBlock = new CapturingOutput();
            dispatchCapturing(server, "et diagnose", notBlock);
            helper.assertTrue(notBlock.text().contains("(not block-payable)"),
                "cost 5 (not a multiple of 9) should report '(not block-payable)' (got: " + notBlock.text() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setUnknownKeyRejected(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        CapturingOutput out = new CapturingOutput();
        int result = dispatchCapturing(server, "et config not_a_real_key true", out);
        helper.assertTrue(result == 0, "setting an unknown key should fail (return 0)");
        helper.assertTrue(out.text().contains("does not exist"),
            "an unknown-key SET should report it does not exist (got: " + out.text() + ")");
        // rejected keys are never created
        helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("not_a_real_key", null) == null,
            "a rejected SET must not create the key");
        helper.complete();
    }

    // configuration keys are case-insensitive
    // commands lowercase keys before lookup
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void keyIsCaseInsensitiveThroughDispatch(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names");
        helper.assertTrue(dispatch(server, "et config CHEAP_NAMES") >= 1,
            "GET of an uppercase key should resolve after lowercasing");
        try {
            helper.assertTrue(dispatch(server, "et config CHEAP_NAMES true") >= 1,
                "SET of an uppercase key should succeed");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "the lowercased key 'cheap_names' should be true after an uppercase SET");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    // et help lists every registered subcommand
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void helpListsAllSubcommands(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        CapturingOutput out = new CapturingOutput();
        int result = dispatchCapturing(server, "et help", out);
        helper.assertTrue(result == 1, "help should return SINGLE_SUCCESS");
        String text = out.text();
        // commands register reload, config, and diagnose
        // help is appended afterward
        for (String sub : new String[]{"config", "reload", "diagnose", "help"}) {
            helper.assertTrue(text.contains("/et " + sub),
                "help should list the '" + sub + "' subcommand (got: " + text + ")");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void commandRegistryRejectsMutation(TestContext helper) {
        int originalSize = ETCommands.getCommands().size();
        boolean rejected = false;
        try {
            ETCommands.getCommands().clear();
        } catch (UnsupportedOperationException e) {
            rejected = true;
        }
        helper.assertTrue(rejected, "the command registry should reject external mutation");
        helper.assertTrue(ETCommands.getCommands().size() == originalSize,
            "a rejected registry mutation must preserve all commands");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void diagnoseOmitsRestartCruft(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        CapturingOutput out = new CapturingOutput();
        int result = dispatchCapturing(server, "et diagnose", out);
        helper.assertTrue(result >= 1, "diagnose should succeed");
        String text = out.text();
        helper.assertTrue(text.contains("mod enabled"),
            "diagnose should still report the 'mod enabled' status label (got: " + text + ")");
        helper.assertFalse(text.contains("restart to take effect"),
            "diagnose must not contain the restart-warning cruft (got: " + text + ")");
        helper.complete();
    }

    // verify get, set, and reset feedback
    // normal set operations omit restart notes
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void feedbackWordingGetSetReset(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names", "better_mending");
        try {
            ETTestHelper.setFeature("cheap_names", false);

            CapturingOutput getOut = new CapturingOutput();
            dispatchCapturing(server, "et config cheap_names", getOut);
            helper.assertTrue(getOut.text().contains("Key 'cheap_names' is set to 'false'."),
                "GET feedback should include the key and current value (got: " + getOut.text() + ")");

            CapturingOutput setOut = new CapturingOutput();
            helper.assertTrue(dispatchCapturing(server, "et config better_mending true", setOut) >= 1,
                "SET should succeed");
            helper.assertTrue(setOut.text().contains("Key 'better_mending' set to 'true'."),
                "SET feedback should include the key and new value (got: " + setOut.text() + ")");
            helper.assertFalse(setOut.text().contains("restart to take effect"),
                "a normal-key SET must not emit the restart note (got: " + setOut.text() + ")");

            CapturingOutput resetOut = new CapturingOutput();
            helper.assertTrue(dispatchCapturing(server, "et config reset better_mending", resetOut) >= 1,
                "reset should succeed");
            helper.assertTrue(resetOut.text().contains("Key 'better_mending' reset to 'false'."),
                "reset feedback should include the key and default value (got: " + resetOut.text() + ")");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void resetAllCountPluralization(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        String[] keys = ETMixinPlugin.getConfig().getKeys().toArray(new String[0]);
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig(keys);
        try {
            dispatch(server, "et config reset all");

            CapturingOutput zero = new CapturingOutput();
            dispatchCapturing(server, "et config reset all", zero);
            helper.assertTrue(zero.text().contains("0 config keys to defaults"),
                "a no-op reset all should report \"0 config keys\" (plural) (got: " + zero.text() + ")");

            dispatch(server, "et config cheap_names true");
            CapturingOutput one = new CapturingOutput();
            dispatchCapturing(server, "et config reset all", one);
            helper.assertTrue(one.text().contains("1 config key to defaults"),
                "reset all after one change should report \"1 config key\" (singular) (got: " + one.text() + ")");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void outOfRangeValuesAccepted(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("anvil_damage_chance", "nte_max_cost");
        try {
            helper.assertTrue(dispatch(server, "et config anvil_damage_chance 2.0") >= 1,
                "a decimal above the documented 0.0-1.0 range should still be accepted");
            helper.assertTrue("2.0".equals(ETMixinPlugin.getConfig().getOrDefault("anvil_damage_chance", null)),
                "anvil_damage_chance should store 2.0 verbatim (no clamping)");

            helper.assertTrue(dispatch(server, "et config anvil_damage_chance -1.0") >= 1,
                "a negative decimal should still be accepted");
            helper.assertTrue("-1.0".equals(ETMixinPlugin.getConfig().getOrDefault("anvil_damage_chance", null)),
                "anvil_damage_chance should store -1.0 verbatim (no clamping)");

            helper.assertTrue(dispatch(server, "et config nte_max_cost -5") >= 1,
                "a negative integer should still be accepted");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", 0) == -5,
                "nte_max_cost should store -5 verbatim (no clamping)");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void commaListStoredVerbatim(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("protection_bypass_types");
        try {
            helper.assertTrue(dispatch(server, "et config protection_bypass_types Magic ,, Wither , MAGIC") >= 1,
                "a mixed-case comma list should still set (LIST is permissive)");
            String stored = ETMixinPlugin.getConfig().getOrDefault("protection_bypass_types", null);
            helper.assertTrue("Magic ,, Wither , MAGIC".equals(stored),
                "the value should preserve case and interior spacing (got '" + stored + "')");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void listKeyClearedViaReset(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("disable_enchantments");
        try {
            helper.assertTrue(dispatch(server, "et config disable_enchantments sharpness,mending") >= 1,
                "setting an enchantment comma list should succeed");
            helper.assertTrue(
                "sharpness,mending".equals(ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", null)),
                "disable_enchantments should hold the comma list");

            helper.assertTrue(dispatch(server, "et config reset disable_enchantments") >= 1,
                "resetting the list key should succeed");
            helper.assertTrue("".equals(ETMixinPlugin.getConfig().getOrDefault("disable_enchantments", null)),
                "reset should restore disable_enchantments to its empty default");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void clientLocalKeySetFromServer(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("roman_numerals");
        try {
            helper.assertTrue(dispatch(server, "et config roman_numerals false") >= 1,
                "setting a client-local key from the server should succeed");
            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("roman_numerals", true),
                "roman_numerals should be false after the SET");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void overviewOmitsReservedOnlyCategory(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        CapturingOutput out = new CapturingOutput();
        int result = dispatchCapturing(server, "et config list", out);
        helper.assertTrue(result >= 1, "overview should succeed");
        String text = out.text();
        helper.assertTrue(text.contains("anvil_tweaks"),
            "overview should list real categories like anvil_tweaks (got: " + text + ")");
        helper.assertFalse(text.contains("config_version"),
            "overview must not expose the reserved config_version key (got: " + text + ")");
        helper.assertFalse(text.contains("internal"),
            "overview must not list the reserved-only 'internal' category (got: " + text + ")");
        helper.complete();
    }

    // per-category page errors differ from all-page errors
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void perCategoryPageOutOfRange(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        // anvil tweaks has ten keys and one page
        // page two is out of range
        helper.assertTrue(dispatch(server, "et config list anvil_tweaks 2") == 0,
            "an out-of-range page for a single category should fail (return 0)");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void modEnabledNoOpStillEmitsRestartNote(TestContext helper) {
        MinecraftServer server = helper.getWorld().getServer();
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("mod_enabled");
        // set mod enabled true before the no-op
        ETTestHelper.setConfigValue("mod_enabled", "true");
        CapturingOutput out = new CapturingOutput();
        try {
            helper.assertTrue(dispatchCapturing(server, "et config mod_enabled true", out) >= 1,
                "no-op SET should still succeed");
            helper.assertTrue(out.text().contains("restart to take effect"),
                "SET mod_enabled emits the restart note even when nothing changed (got: " + out.text() + ")");
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(originalConfig);
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    // helpers

    private static int dispatch(MinecraftServer server, String command) {
        try {
            return server.getCommandManager().getDispatcher().execute(command, server.getCommandSource());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("command dispatch failed: " + command, e);
        }
    }

    /**
     * dispatches through a level-four source records every feedback and error
     * message
     */
    private static int dispatchCapturing(MinecraftServer server, String command, CapturingOutput out) {
        ServerCommandSource source = server.getCommandSource().withOutput(out);
        try {
            return server.getCommandManager().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("command dispatch failed: " + command, e);
        }
    }

    /**
     * dispatches a rejected set and checks the value remains unchanged
     */
    private static void assertRejectedUnchanged(TestContext helper, MinecraftServer server, String key, String value) {
        String before = ETMixinPlugin.getConfig().getOrDefault(key, null);
        int result = dispatch(server, "et config " + key + " " + value);
        String after = ETMixinPlugin.getConfig().getOrDefault(key, null);
        helper.assertTrue(result == 0,
            "set " + key + "=" + value + " should be rejected (return 0) but returned " + result);
        helper.assertTrue(Objects.equals(before, after),
            key + " must be unchanged after a rejected set (before=" + before + " after=" + after + ")");
    }

    private static List<String> completions(MinecraftServer server, String command) {
        ParseResults<ServerCommandSource> parse = server.getCommandManager().getDispatcher().parse(command,
            server.getCommandSource());
        Suggestions suggestions = server.getCommandManager().getDispatcher().getCompletionSuggestions(parse).join();
        return suggestions.getList().stream().map(Suggestion::getText).toList();
    }

    private static final class CapturingOutput implements CommandOutput {
        final List<Text> messages = new ArrayList<>();

        @Override
        public void sendMessage(Text message) {
            messages.add(message);
        }

        @Override
        public boolean shouldReceiveFeedback() {
            return true;
        }

        @Override
        public boolean shouldTrackOutput() {
            return true;
        }

        @Override
        public boolean shouldBroadcastConsoleToOps() {
            return false;
        }

        String text() {
            StringBuilder sb = new StringBuilder();
            for (Text t : messages) {
                sb.append(t.getString()).append('\n');
            }
            return sb.toString();
        }
    }
}
