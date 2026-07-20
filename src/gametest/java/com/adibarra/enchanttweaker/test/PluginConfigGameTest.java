package com.adibarra.enchanttweaker.test;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADConfig;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfigGameTest implements FabricGameTest {


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void modDisabledGatesFeatureMixins(TestContext helper) {
        ETTestHelper.setFeature("cheap_names", true);
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);

        helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 10,
            "baseline: capmod should raise Sharpness getMaxLevel to 10 while mod_enabled=true (got "
                + Enchantments.SHARPNESS.getMaxLevel() + ")");

        // set last so every cache is cleared with mod_enabled=false in effect
        ETTestHelper.setConfigValue("mod_enabled", "false");
        try {
            helper.assertFalse(ETMixinPlugin.getMixinConfig("CheapNamesMixin"),
                "cheap_names mixin should be gated off when mod_enabled=false");
            helper.assertFalse(ETMixinPlugin.getMixinConfig("MoreProtectionMixin"),
                "more_protection mixin should be gated off when mod_enabled=false");
            helper.assertFalse(ETMixinPlugin.getMixinConfig("CapmodMixin"),
                "capmod mixin should be gated off when mod_enabled=false");

            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "mod_enabled=false must gate the capmod mixin: Sharpness getMaxLevel should fall back "
                    + "to vanilla 5 (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");

            // paired with the real getMaxLevel==5 above: getCapmodLevel ignores mod_enabled and still
            // returns the configured cap 10. The pair documents that the gate lives in getMixinConfig,
            // not in getCapmodLevel
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 5) == 10,
                "getCapmodLevel should ignore mod_enabled and return the configured cap 10 (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 5) + ")");
        } finally {
            ETTestHelper.setConfigValue("mod_enabled", "true");
            ETTestHelper.setFeature("cheap_names", false);
            ETTestHelper.setFeature("more_protection", false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        // with mod_enabled restored and capmod reset off, the gate lifts and getMaxLevel is
        // back to vanilla 5 - confirms the finally cleaned up the raised cap that would otherwise
        // leak into CapmodGameTest
        helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("mod_enabled", false),
            "mod_enabled must be restored to true after the test");
        helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
            "after restore (capmod off) Sharpness getMaxLevel should be vanilla 5 (got "
                + Enchantments.SHARPNESS.getMaxLevel() + ")");
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void setAllAndPersistWritesToDisk(TestContext helper) {
        String path = ETMixinPlugin.getConfig().getConfigPath();
        helper.assertTrue(path != null, "config path should be resolvable");
        try {
            ETMixinPlugin.getConfig().setAllAndPersist(Map.of("cheap_names", "true"));
            ETMixinPlugin.clearCaches();

            String fileText = Files.readString(Path.of(path));
            helper.assertTrue(fileText.contains("cheap_names=true"),
                "the run-dir config file should contain the persisted value cheap_names=true");
            // in-memory state must agree with what was written
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "in-memory cheap_names should be true after setAllAndPersist");
        } catch (java.io.IOException e) {
            throw new RuntimeException("failed to read config file", e);
        } finally {
            ETMixinPlugin.getConfig().setAllAndPersist(Map.of("cheap_names", "false"));
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void setAllAndPersistAppendsUnwrittenKey(TestContext helper) {
        String path = ETMixinPlugin.getConfig().getConfigPath();
        helper.assertTrue(path != null, "config path should be resolvable");
        Path configPath = Path.of(path);
        String originalSturdyValue = ETMixinPlugin.getConfig().getOrDefault("sturdy_anvils", "false");

        String original;
        try {
            original = Files.readString(configPath);
        } catch (java.io.IOException e) {
            throw new RuntimeException("failed to read config file", e);
        }
        // precondition: the run-dir config must actually contain the key we're about to remove,
        // otherwise the append branch wouldn't be the thing under test
        helper.assertTrue(
            original.lines().anyMatch(l -> l.trim().toLowerCase().startsWith("sturdy_anvils=")),
            "precondition: run-dir config should contain a sturdy_anvils= line");

        try {
            // hand-remove the sturdy_anvils line (simulating a user-truncated config file)
            List<String> keptLines = original.lines()
                .filter(l -> !l.trim().toLowerCase().startsWith("sturdy_anvils="))
                .toList();
            Files.writeString(configPath, String.join("\n", keptLines));
            helper.assertFalse(
                Files.readString(configPath).lines()
                    .anyMatch(l -> l.trim().toLowerCase().startsWith("sturdy_anvils=")),
                "the sturdy_anvils line should be gone before setAllAndPersist");

            // `setAllAndPersist` loads the file fresh, finds no matching line, and hits the append branch
            ETMixinPlugin.getConfig().setAllAndPersist(Map.of("sturdy_anvils", "false"));
            ETMixinPlugin.clearCaches();

            String rewritten = Files.readString(configPath);
            // the append branch writes an un-indented "sturdy_anvils=false" line at the end of the file
            helper.assertTrue(
                rewritten.lines().anyMatch(l -> l.trim().equals("sturdy_anvils=false")),
                "setAllAndPersist should append a fresh sturdy_anvils=false line when none exists");
            // in-memory state must agree with what was written
            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("sturdy_anvils", true),
                "in-memory sturdy_anvils should be false after setAllAndPersist");
        } catch (java.io.IOException e) {
            throw new RuntimeException("config file round-trip failed", e);
        } finally {
            // restore the original file verbatim and re-sync the in-memory value from it
            try {
                Files.writeString(configPath, original);
            } catch (java.io.IOException e) {
                throw new RuntimeException("failed to restore config file", e);
            }
            ETMixinPlugin.getConfig().setAll(Map.of("sturdy_anvils", originalSturdyValue));
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void reloadClearsCaches(TestContext helper) {
        try {
            ETTestHelper.setEnchantCap("sharpness", 5);   // setAll + clearCaches
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 5,
                "primed capmod level should be 5 (got " + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");

            // mutate the live config in-memory but do NOT clear the cache: the cached 5 must survive
            ETMixinPlugin.getConfig().setAll(Map.of("sharpness", "7"));
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 5,
                "CAPMOD_CACHE is sticky: level should still read the cached 5 until a cache-clearing reload (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");

            // `reloadConfig` must clear the cache AND swap in the on-disk config (sharpness=-1 default),
            // so cap<0 falls through to the vanilla passthrough level 10
            ETMixinPlugin.reloadConfig();
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 10,
                "reloadConfig must clear CAPMOD_CACHE and reload disk (sharpness=-1) -> vanilla passthrough 10 (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");
        } finally {
            // `reloadConfig` restores the pristine on-disk baseline and clears caches
            ETMixinPlugin.reloadConfig();
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void setAllAndPersistEdgeCases(TestContext helper) {
        Path tmp = configDir().resolve("et-persist-edge-test.properties");
        try {
            Files.writeString(tmp, "existing_key=orig\n");
            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), BOGUS_BUNDLED);

            String before = Files.readString(tmp);
            cfg.setAllAndPersist(Map.of());   // empty map -> no-op
            cfg.setAllAndPersist(null);       // null -> no-op
            helper.assertTrue(before.equals(Files.readString(tmp)),
                "empty-map and null setAllAndPersist must be no-ops (file unchanged)");

            // replace-in-place on the existing line
            cfg.setAllAndPersist(Map.of("existing_key", "changed"));
            helper.assertTrue(Files.readString(tmp).contains("existing_key=changed"),
                "setAllAndPersist should replace the existing_key value in place");
            helper.assertTrue("changed".equals(cfg.getOrDefault("existing_key", "x")),
                "in-memory existing_key should be 'changed'");

            // mixed-case key: lowercased before storage, appended (no matching line exists)
            cfg.setAllAndPersist(Map.of("MixedKey", "v"));
            helper.assertTrue(Files.readString(tmp).lines().anyMatch(l -> l.trim().equals("mixedkey=v")),
                "mixed-case key should be lowercased and appended as mixedkey=v");
            helper.assertTrue(cfg.getKeys().contains("mixedkey"),
                "in-memory key should be the lowercased 'mixedkey'");
            helper.assertFalse(cfg.getKeys().contains("MixedKey"),
                "the raw mixed-case key must not be stored");

            // unicode value written verbatim (UTF-8) and round-tripped
            cfg.setAllAndPersist(Map.of("uni_key", "café🗡️"));
            helper.assertTrue(Files.readString(tmp).contains("uni_key=café🗡️"),
                "unicode value should be written verbatim");
            helper.assertTrue("café🗡️".equals(cfg.getOrDefault("uni_key", "x")),
                "in-memory unicode value should round-trip");

            // null values in the map are sanitized away; non-null siblings still persist
            Map<String, String> withNull = new HashMap<>();
            withNull.put("null_val_key", null);
            withNull.put("kept_key", "kept");
            cfg.setAllAndPersist(withNull);
            helper.assertFalse(cfg.getKeys().contains("null_val_key"),
                "a null-valued entry must be dropped, not stored");
            helper.assertTrue("kept".equals(cfg.getOrDefault("kept_key", "x")),
                "a non-null sibling of a dropped null entry should still persist");
        } catch (IOException e) {
            throw new RuntimeException("setAllAndPersistEdgeCases I/O failed", e);
        } finally {
            deleteQuietly(tmp);
        }
        helper.complete();
    }

    // local helpers (private to this owned test file)

    private static final String BOGUS_BUNDLED = "enchanttweaker-test/NONEXISTENT-defaults.properties";

    private static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
