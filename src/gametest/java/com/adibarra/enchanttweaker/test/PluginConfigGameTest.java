package com.adibarra.enchanttweaker.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADConfig;

public class PluginConfigGameTest implements FabricGameTest {

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void modDisabledGatesFeatureMixins(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("cheap_names", "more_protection",
            "capmod_enabled", "sharpness", "mod_enabled");
        try {
            ETTestHelper.setConfigValue("mod_enabled", "true");
            ETTestHelper.setFeature("cheap_names", true);
            ETTestHelper.setFeature("more_protection", true);
            ETTestHelper.setCapmod(true);
            ETTestHelper.setEnchantCap("sharpness", 10);

            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 10,
                "baseline: capmod should raise Sharpness getMaxLevel to 10 while mod_enabled=true (got "
                    + Enchantments.SHARPNESS.getMaxLevel() + ")");

            // set this last to clear caches under mod_enabled=false
            ETTestHelper.setConfigValue("mod_enabled", "false");
            helper.assertFalse(ETMixinPlugin.getMixinConfig("CheapNamesMixin"),
                "cheap_names mixin should be gated off when mod_enabled=false");
            helper.assertFalse(ETMixinPlugin.getMixinConfig("MoreProtectionMixin"),
                "more_protection mixin should be gated off when mod_enabled=false");
            helper.assertFalse(ETMixinPlugin.getMixinConfig("CapmodMixin"),
                "capmod mixin should be gated off when mod_enabled=false");

            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "mod_enabled=false must gate the capmod mixin: Sharpness getMaxLevel should fall back "
                    + "to vanilla 5 (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");

            // this confirms getMixinConfig handles the mod_enabled gate
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 5) == 10,
                "getCapmodLevel should ignore mod_enabled and return the configured cap 10 (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 5) + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setAllAndPersistWritesToDisk(TestContext helper) {
        String path = ETMixinPlugin.getConfig().getConfigPath();
        helper.assertTrue(path != null, "config path should be resolvable");
        helper.assertTrue(ETMixinPlugin.getConfig().getKeys().contains("cheap_names"),
            "precondition: loaded config should contain cheap_names");
        String originalCheapNames = ETMixinPlugin.getConfig().getOrDefault("cheap_names", "false");
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
            ETMixinPlugin.getConfig().setAllAndPersist(Map.of("cheap_names", originalCheapNames));
            ETMixinPlugin.clearCaches();
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
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
        // confirm the source file contains sturdy_anvils
        // otherwise append behavior cannot be tested
        helper.assertTrue(original.lines().anyMatch(l -> l.trim().toLowerCase().startsWith("sturdy_anvils=")),
            "precondition: run-dir config should contain a sturdy_anvils= line");

        try {
            // hand-remove the sturdy_anvils line (simulating a user-truncated config file)
            List<String> keptLines = original.lines().filter(l -> !l.trim().toLowerCase().startsWith("sturdy_anvils="))
                .toList();
            Files.writeString(configPath, String.join("\n", keptLines));
            helper.assertFalse(
                Files.readString(configPath).lines().anyMatch(l -> l.trim().toLowerCase().startsWith("sturdy_anvils=")),
                "the sturdy_anvils line should be gone before setAllAndPersist");

            // setAllAndPersist appends when no matching line exists
            ETMixinPlugin.getConfig().setAllAndPersist(Map.of("sturdy_anvils", "false"));
            ETMixinPlugin.clearCaches();

            String rewritten = Files.readString(configPath);
            // the append branch writes sturdy_anvils=false
            helper.assertTrue(rewritten.lines().anyMatch(l -> l.trim().equals("sturdy_anvils=false")),
                "setAllAndPersist should append a fresh sturdy_anvils=false line when none exists");
            // in-memory state must agree with what was written
            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("sturdy_anvils", true),
                "in-memory sturdy_anvils should be false after setAllAndPersist");
        } catch (java.io.IOException e) {
            throw new RuntimeException("config file round-trip failed", e);
        } finally {
            // restore the original file verbatim and re-sync the in-memory value
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

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void reloadClearsCaches(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper
            .snapshotConfig(ETMixinPlugin.getConfig().getKeys().toArray(new String[0]));
        String configPath = ETMixinPlugin.getConfig().getConfigPath();
        helper.assertTrue(configPath != null, "config path should be resolvable");
        Path diskPath = Path.of(configPath);
        String originalDisk;
        try {
            originalDisk = Files.readString(diskPath);
        } catch (IOException e) {
            throw new RuntimeException("failed to snapshot config file", e);
        }

        try {
            ETTestHelper.setEnchantCap("sharpness", 5); // setAll + clearCaches
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 5,
                "primed capmod level should be 5 (got " + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");

            // establish the on-disk value before changing only the in-memory config
            ETMixinPlugin.getConfig().setAllAndPersist(Map.of("sharpness", "-1"));
            helper.assertTrue(Files.readString(diskPath).lines().anyMatch(line -> line.trim().equals("sharpness=-1")),
                "reload precondition: disk should contain sharpness=-1");
            ETMixinPlugin.getConfig().setAll(Map.of("sharpness", "7"));
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 5,
                "CAPMOD_CACHE is sticky: level should still read the cached 5 until a cache-clearing reload (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");

            // reloadConfig clears the cache and reloads disk
            ETMixinPlugin.reloadConfig();
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 10,
                "reloadConfig must clear CAPMOD_CACHE and reload disk (sharpness=-1) -> vanilla passthrough 10 (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");
        } catch (IOException e) {
            throw new RuntimeException("reload cache test I/O failed", e);
        } finally {
            try {
                Files.writeString(diskPath, originalDisk);
            } catch (IOException e) {
                throw new RuntimeException("failed to restore config file", e);
            }
            ETMixinPlugin.reloadConfig();
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setAllAndPersistEdgeCases(TestContext helper) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile(configDir(), "et-persist-edge-test-", ".properties");
            Files.writeString(tmp,
                "existing_key=orig\nspaced_key = OriginalCase\ncase_key=MiXeD\nduplicate_key=first\nduplicate_key=second\n");
            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), BOGUS_BUNDLED);
            helper.assertTrue("MiXeD".equals(cfg.getOrDefault("case_key", "")),
                "config parsing should preserve case-sensitive values");
            helper.assertTrue(cfg.set("CASE_KEY", "ChangedCase"),
                "set should normalize mixed-case keys before updating");
            helper.assertTrue("ChangedCase".equals(cfg.getOrDefault("case_key", "")),
                "set should update the normalized key");
            Map<String, String> mixedValues = new HashMap<>();
            mixedValues.put("BOOL_KEY", "true");
            mixedValues.put("INT_KEY", "42");
            mixedValues.put("DOUBLE_KEY", "2.5");
            mixedValues.put(null, "ignored");
            mixedValues.put("NULL_VALUE", null);
            cfg.setAll(mixedValues);
            helper.assertTrue(cfg.getOrDefault("bool_key", false), "setAll should normalize boolean keys");
            helper.assertTrue(cfg.getOrDefault("int_key", 0) == 42, "setAll should normalize integer keys");
            helper.assertTrue(cfg.getOrDefault("double_key", 0.0) == 2.5, "setAll should normalize decimal keys");
            cfg.setAll(null);
            helper.assertTrue(cfg.set("spaced_key", "UpdatedValue"),
                "set should recognize assignments with whitespace around the key");
            String spacedText = Files.readString(tmp);
            helper.assertTrue(spacedText.contains("spaced_key = UpdatedValue"),
                "set should replace a whitespace-spaced assignment in place");
            helper.assertTrue(spacedText.lines().filter(line -> line.contains("spaced_key")).count() == 1,
                "set should not append a duplicate whitespace-spaced assignment");
            helper.assertTrue(cfg.set("duplicate_key", "updated"), "set should update duplicate assignments");
            String duplicateText = Files.readString(tmp);
            helper.assertTrue(duplicateText.lines().filter(line -> line.equals("duplicate_key=updated")).count() == 2,
                "set should update every duplicate assignment");
            ADConfig reloaded = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), BOGUS_BUNDLED);
            helper.assertTrue("updated".equals(reloaded.getOrDefault("duplicate_key", "")),
                "duplicate assignments should retain the updated value after reload");

            String before = Files.readString(tmp);
            cfg.setAllAndPersist(Map.of()); // empty map -> no-op
            cfg.setAllAndPersist(null); // null -> no-op
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
            helper.assertTrue(cfg.getKeys().contains("mixedkey"), "in-memory key should be the lowercased 'mixedkey'");
            helper.assertFalse(cfg.getKeys().contains("MixedKey"), "the raw mixed-case key must not be stored");

            // unicode value written verbatim (UTF-8) and round-tripped
            cfg.setAllAndPersist(Map.of("uni_key", "café🗡️"));
            helper.assertTrue(Files.readString(tmp).contains("uni_key=café🗡️"),
                "unicode value should be written verbatim");
            helper.assertTrue("café🗡️".equals(cfg.getOrDefault("uni_key", "x")),
                "in-memory unicode value should round-trip");

            // null values are dropped while valid siblings persist
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

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void setAllAndPersistSerializesConcurrentUpdates(TestContext helper) {
        Path tmp = null;
        Thread left = null;
        Thread right = null;
        try {
            tmp = Files.createTempFile(configDir(), "et-persist-race-test-", ".properties");
            Files.writeString(tmp, "# pad\n".repeat(10000) + "left=0\nright=0\n");
            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), BOGUS_BUNDLED);

            for (int round = 1; round <= 64; round++) {
                cfg.setAllAndPersist(Map.of("left", "0", "right", "0"));
                String value = Integer.toString(round);
                CountDownLatch start = new CountDownLatch(1);
                AtomicReference<Throwable> failure = new AtomicReference<>();
                left = concurrentUpdate(start, failure, () -> cfg.setAllAndPersist(Map.of("left", value)));
                right = concurrentUpdate(start, failure, () -> cfg.setAllAndPersist(Map.of("right", value)));
                left.start();
                right.start();
                start.countDown();
                left.join(CONCURRENT_UPDATE_TIMEOUT_MILLIS);
                if (failure.get() != null)
                    throw new RuntimeException("concurrent persistence failed", failure.get());
                right.join(CONCURRENT_UPDATE_TIMEOUT_MILLIS);
                if (failure.get() != null)
                    throw new RuntimeException("concurrent persistence failed", failure.get());
                if (left.isAlive() || right.isAlive()) {
                    throw new RuntimeException("concurrent persistence worker timed out (left alive=" + left.isAlive()
                        + ", right alive=" + right.isAlive() + ")");
                }

                String persisted = Files.readString(tmp);
                helper.assertTrue(persisted.contains("left=" + value) && persisted.contains("right=" + value),
                    "concurrent updates should both persist in round " + round);
            }
        } catch (InterruptedException e) {
            interruptWorkers(left, right);
            Thread.currentThread().interrupt();
            throw new RuntimeException("concurrent persistence test interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("concurrent persistence test failed", e);
        } finally {
            cleanupWorkers(left, right);
            deleteQuietly(tmp);
        }
        helper.complete();
    }

    private static Thread concurrentUpdate(CountDownLatch start, AtomicReference<Throwable> failure, Runnable update) {
        return new Thread(() -> {
            try {
                start.await();
                update.run();
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });
    }

    private static void interruptWorkers(Thread... workers) {
        for (Thread worker : workers) {
            if (worker != null && worker.isAlive())
                worker.interrupt();
        }
    }

    private static void cleanupWorkers(Thread... workers) {
        interruptWorkers(workers);
        for (Thread worker : workers) {
            if (worker == null || !worker.isAlive())
                continue;
            try {
                worker.join(WORKER_CLEANUP_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static final long CONCURRENT_UPDATE_TIMEOUT_MILLIS = 10_000L;
    private static final long WORKER_CLEANUP_TIMEOUT_MILLIS = 1_000L;

    // local test helpers

    private static final String BOGUS_BUNDLED = "enchanttweaker-test/NONEXISTENT-defaults.properties";

    private static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    private static void deleteQuietly(Path path) {
        if (path == null || !configDir().equals(path.getParent()) || !path.getFileName().toString().startsWith("et-")) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
