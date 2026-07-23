package com.adibarra.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

public class ADConfigGameTest implements FabricGameTest {

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void valueMigrationPersistsAcrossReload(TestContext helper) {
        Path configPath;
        try {
            configPath = Files.createTempFile(FabricLoader.getInstance().getConfigDir(), "et-migvalue-test-",
                ".properties");
        } catch (IOException e) {
            throw new RuntimeException("failed to create isolated config fixture", e);
        }
        try {
            Files.writeString(configPath, "config_version=0\ntransformed_value=before\n");
            ADConfig.Migration migration = new ADConfig.Migration(1, map -> map.put("transformed_value", "after"));
            ADConfig migrated = new ADConfig("ADConfigGameTest", configPath.getFileName().toString(),
                "enchanttweaker-test/defaults-value-migration.properties", List.of(migration));

            helper.assertTrue("after".equals(migrated.getOrDefault("transformed_value", "missing")),
                "migration should transform the in-memory value");
            String persisted = Files.readString(configPath);
            helper.assertTrue(persisted.contains("config_version=1"), "migration should persist the current version");
            helper.assertTrue(persisted.contains("transformed_value=after"),
                "migration should persist transformed values");

            ADConfig reloaded = new ADConfig("ADConfigGameTest", configPath.getFileName().toString(),
                "enchanttweaker-test/defaults-value-migration.properties");
            helper.assertTrue("after".equals(reloaded.getOrDefault("transformed_value", "missing")),
                "transformed value should survive reload");
        } catch (IOException e) {
            throw new RuntimeException("value migration persistence test failed", e);
        } finally {
            deleteQuietly(configPath);
        }
        helper.complete();
    }

    private static void deleteQuietly(Path path) {
        if (path == null || !FabricLoader.getInstance().getConfigDir().equals(path.getParent())
            || path.getFileName() == null || !path.getFileName().toString().startsWith("et-")) {
            return;
        }
        try {
            // deleteIfExists never follows a symlink, including a broken one.
            if (Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
