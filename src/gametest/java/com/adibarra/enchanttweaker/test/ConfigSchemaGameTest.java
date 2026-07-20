package com.adibarra.enchanttweaker.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADConfig;

public class ConfigSchemaGameTest implements FabricGameTest {

    // typeOf inference

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaTypeInference(TestContext helper) {
        helper.assertTrue(ETConfigSchema.typeOf("cheap_names") == ETConfigSchema.ValueType.BOOLEAN,
            "cheap_names should infer BOOLEAN");
        helper.assertTrue(ETConfigSchema.typeOf("nte_max_cost") == ETConfigSchema.ValueType.INTEGER,
            "nte_max_cost should infer INTEGER");
        helper.assertTrue(ETConfigSchema.typeOf("anvil_damage_chance") == ETConfigSchema.ValueType.DECIMAL,
            "anvil_damage_chance should infer DECIMAL");
        helper.assertTrue(ETConfigSchema.typeOf("disable_enchantments") == ETConfigSchema.ValueType.LIST,
            "disable_enchantments should infer LIST");
        helper.complete();
    }

    // isValid per type

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaIsValid(TestContext helper) {
        // boolean accepts only true/false
        helper.assertTrue(ETConfigSchema.isValid("cheap_names", "true"), "cheap_names=true should be valid");
        helper.assertTrue(ETConfigSchema.isValid("cheap_names", "false"), "cheap_names=false should be valid");
        helper.assertTrue(!ETConfigSchema.isValid("cheap_names", "banana"), "cheap_names=banana should be invalid");

        // integer must int-parse
        helper.assertTrue(ETConfigSchema.isValid("nte_max_cost", "100"), "nte_max_cost=100 should be valid");
        helper.assertTrue(!ETConfigSchema.isValid("nte_max_cost", "banana"), "nte_max_cost=banana should be invalid");
        helper.assertTrue(!ETConfigSchema.isValid("nte_max_cost", "1.5"), "nte_max_cost=1.5 should be invalid");

        // decimal must double-parse
        helper.assertTrue(ETConfigSchema.isValid("anvil_damage_chance", "0.5"),
            "anvil_damage_chance=0.5 should be valid");
        helper.assertTrue(!ETConfigSchema.isValid("anvil_damage_chance", "banana"),
            "anvil_damage_chance=banana should be invalid");
        helper.assertTrue(!ETConfigSchema.isValid("anvil_damage_chance", "Infinity"),
            "infinite decimal should be invalid");
        helper.assertTrue(!ETConfigSchema.isValid("anvil_damage_chance", "NaN"), "NaN decimal should be invalid");

        // list is always valid
        helper.assertTrue(ETConfigSchema.isValid("disable_enchantments", "sharpness,mending"),
            "list value should be valid");
        helper.assertTrue(ETConfigSchema.isValid("disable_enchantments", ""), "empty list value should be valid");
        helper.complete();
    }

    // unknown key is allowed

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaUnknownKey(TestContext helper) {
        helper.assertTrue(ETConfigSchema.typeOf("not_a_real_key") == null, "unknown key should have a null type");
        helper.assertTrue(ETConfigSchema.isValid("not_a_real_key", "anything"),
            "unknown key should be permissively valid");
        helper.complete();
    }

    // expected() hints

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaExpectedHints(TestContext helper) {
        helper.assertTrue(!ETConfigSchema.expected("cheap_names").isEmpty(), "boolean key should have a hint");
        helper.assertTrue(!ETConfigSchema.expected("nte_max_cost").isEmpty(), "integer key should have a hint");
        helper.assertTrue(!ETConfigSchema.expected("anvil_damage_chance").isEmpty(), "decimal key should have a hint");
        helper.assertTrue(!ETConfigSchema.expected("disable_enchantments").isEmpty(), "list key should have a hint");
        helper.complete();
    }

    // reserved keys

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaReservedKeys(TestContext helper) {
        helper.assertTrue(ETConfigSchema.isReserved("config_version"), "config_version should be reserved");
        helper.assertTrue(!ETConfigSchema.isReserved("cheap_names"), "cheap_names should not be reserved");

        helper.assertTrue(!ETConfigSchema.defaults().containsKey("config_version"),
            "defaults() should exclude reserved keys");
        helper.assertTrue(!ETConfigSchema.keysIn("internal").contains("config_version"),
            "keysIn() should exclude reserved keys");
        helper.complete();
    }

    // bundled defaults

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaDefaults(TestContext helper) {
        helper.assertTrue("false".equals(ETConfigSchema.defaultOf("cheap_names")),
            "cheap_names default should be false");
        helper.assertTrue("2147483647".equals(ETConfigSchema.defaultOf("nte_max_cost")),
            "nte_max_cost default should be 2147483647");
        helper.assertTrue("1.33".equals(ETConfigSchema.defaultOf("pw_cost_multiplier")),
            "pw_cost_multiplier default should be 1.33");
        helper.assertTrue(ETConfigSchema.defaultOf("not_a_real_key") == null, "unknown key should have a null default");
        helper.complete();
    }

    // categories in file order

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaCategories(TestContext helper) {
        List<String> expected = List.of("master_switch", "anvil_tweaks", "enhanced_enchantments", "other_tweaks",
            "villager_trade_limits", "modify_max_enchantment_levels");
        helper.assertTrue(ETConfigSchema.categories().equals(expected),
            "categories() should equal " + expected + " but was " + ETConfigSchema.categories());
        helper.assertTrue(!ETConfigSchema.categories().contains("internal"),
            "internal category should be hidden (no non-reserved keys)");
        helper.complete();
    }

    // keys within a category

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaKeysIn(TestContext helper) {
        List<String> anvil = ETConfigSchema.keysIn("anvil_tweaks");
        List<String> expectedAnvil = List.of("cheap_names", "not_too_expensive", "nte_max_cost", "prior_work_cheaper",
            "pw_cost_multiplier", "prior_work_free", "sturdy_anvils", "anvil_damage_chance", "anvil_repair",
            "anvil_repair_ingot_cost");
        helper.assertTrue(anvil.equals(expectedAnvil),
            "keysIn(anvil_tweaks) should equal " + expectedAnvil + " but was " + anvil);
        helper.assertTrue("villager_trade_limits".equals(ETConfigSchema.categoryOf("trade_mending")),
            "trade_mending should be in villager_trade_limits");
        helper.assertTrue(ETConfigSchema.keysIn("not_a_real_slug").isEmpty(),
            "unknown slug should yield an empty list");
        helper.complete();
    }

    // migration ordering

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void migrationOrdering(TestContext helper) {
        // `toVersion` 1: rename old_name -> new_name; toVersion 2: rewrite color. Each
        // appends to
        // "trace" so the resulting order proves ascending-toVersion application
        ADConfig.Migration m1 = new ADConfig.Migration(1, map -> {
            map.put("new_name", map.remove("old_name"));
            map.put("trace", map.getOrDefault("trace", "") + "A");
        });
        ADConfig.Migration m2 = new ADConfig.Migration(2, map -> {
            map.put("color", "green");
            map.put("trace", map.getOrDefault("trace", "") + "B");
        });

        Map<String, String> config = new HashMap<>();
        config.put("old_name", "x");
        config.put("color", "red");
        config.put("trace", "");

        // pass reversed so the ascending sort inside applyMigrations is exercised
        ADConfig.applyMigrations(config, List.of(m2, m1), 0, 2);

        helper.assertTrue("x".equals(config.get("new_name")), "old_name should be renamed to new_name");
        helper.assertTrue(!config.containsKey("old_name"), "old_name should be removed by the rename");
        helper.assertTrue("green".equals(config.get("color")), "color should be rewritten to green");
        helper.assertTrue("AB".equals(config.get("trace")),
            "migrations should apply in ascending toVersion order (trace=AB)");
        helper.assertTrue("2".equals(config.get(ADConfig.VERSION_KEY)), "VERSION_KEY should be stamped 2");
        helper.complete();
    }

    // migration skip / downgrade

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void migrationSkip(TestContext helper) {
        ADConfig.Migration m1 = new ADConfig.Migration(1, map -> map.put("new_name", map.remove("old_name")));
        ADConfig.Migration m2 = new ADConfig.Migration(2, map -> map.put("color", "green"));
        List<ADConfig.Migration> migrations = List.of(m1, m2);

        // stored 1 -> only toVersion-2 applies
        Map<String, String> a = new HashMap<>();
        a.put("old_name", "x");
        a.put("color", "red");
        ADConfig.applyMigrations(a, migrations, 1, 2);
        helper.assertTrue(a.containsKey("old_name"), "stored 1: rename (toVersion 1) should be skipped");
        helper.assertTrue(!a.containsKey("new_name"), "stored 1: new_name should not be created");
        helper.assertTrue("green".equals(a.get("color")), "stored 1: toVersion-2 rewrite should apply");
        helper.assertTrue("2".equals(a.get(ADConfig.VERSION_KEY)), "stored 1: VERSION_KEY should be stamped 2");

        // stored 2 == current -> no migration applies
        Map<String, String> b = new HashMap<>();
        b.put("color", "red");
        ADConfig.applyMigrations(b, migrations, 2, 2);
        helper.assertTrue("red".equals(b.get("color")), "stored 2: no transform should run");
        helper.assertTrue("2".equals(b.get(ADConfig.VERSION_KEY)), "stored 2: VERSION_KEY should be stamped 2");

        // stored 3 > current -> downgrade: no transform, keep values, stamp current
        Map<String, String> c = new HashMap<>();
        c.put("color", "red");
        ADConfig.applyMigrations(c, migrations, 3, 2);
        helper.assertTrue("red".equals(c.get("color")), "downgrade: values should be kept as-is");
        helper.assertTrue("2".equals(c.get(ADConfig.VERSION_KEY)),
            "downgrade: VERSION_KEY should be stamped current (2)");
        helper.complete();
    }

    // live config version

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void liveConfigVersion(TestContext helper) {
        // proves the bundled config_version migration ran against the gametest run-dir
        // config
        helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("config_version", 0) == 1,
            "loaded config_version should be 1");
        helper.complete();
    }

    // isolated migrateConfig key-diff

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void migrateConfigKeyDiff(TestContext helper) {
        Path tmp = configDir().resolve("et-migdiff-test.properties");
        try {
            Files.writeString(tmp, "cheap_names=true\nobsolete_key=stale_value\n");

            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), REAL_BUNDLED);

            // preserved: the user's customized value survives the migration
            helper.assertTrue(cfg.getOrDefault("cheap_names", false),
                "migrateConfig should preserve the user's customized cheap_names=true");
            // added: the missing bundled key is added at its bundled default
            // (god_armor=false)
            helper.assertTrue(!cfg.getOrDefault("god_armor", true),
                "migrateConfig should add the missing god_armor key at its bundled default (false)");
            // dropped: the stale key is removed from the in-memory map
            helper.assertTrue(!cfg.getKeys().contains("obsolete_key"),
                "migrateConfig should drop the stale obsolete_key");

            // on-disk: the rewritten file carries the added key and no longer mentions the
            // stale key
            String fileText = Files.readString(tmp);
            helper.assertTrue(fileText.contains("god_armor="),
                "rewritten file should contain the added god_armor= line");
            helper.assertTrue(!fileText.contains("obsolete_key"),
                "rewritten file should not contain the dropped obsolete_key");
        } catch (IOException e) {
            throw new RuntimeException("migrateConfigKeyDiff I/O failed", e);
        } finally {
            deleteQuietly(tmp);
        }
        helper.complete();
    }

    // isolated applyVersionMigrations live caller + stamp

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void migrateConfigVersionStamp(TestContext helper) {
        Path tmp = configDir().resolve("et-migver-test.properties");
        try {
            String bundled = readResourceText(REAL_BUNDLED);
            helper.assertTrue(bundled != null, "bundled defaults resource must be present");
            Files.writeString(tmp, bundled.replace("config_version=1", "config_version=0"));

            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), REAL_BUNDLED);

            helper.assertTrue(cfg.getOrDefault("config_version", -1) == 1,
                "applyVersionMigrations should bump config_version 0 -> 1");
            String fileText = Files.readString(tmp);
            helper.assertTrue(fileText.contains("config_version=1"),
                "the version bump should be persisted to disk (config_version=1)");
        } catch (IOException e) {
            throw new RuntimeException("migrateConfigVersionStamp I/O failed", e);
        } finally {
            deleteQuietly(tmp);
        }
        helper.complete();
    }

    // isolated version parse + machinery-disabled paths

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void migrateConfigParsePaths(TestContext helper) {
        // (a) a non-numeric stored version parses to 0 and still migrates up to the
        // bundled
        // version without crashing
        Path tmpA = configDir().resolve("et-migparse-abc.properties");
        try {
            String bundled = readResourceText(REAL_BUNDLED);
            helper.assertTrue(bundled != null, "bundled defaults resource must be present");
            Files.writeString(tmpA, bundled.replace("config_version=1", "config_version=abc"));

            ADConfig cfgA = new ADConfig(EnchantTweaker.MOD_NAME, tmpA.getFileName().toString(), REAL_BUNDLED);
            helper.assertTrue(cfgA.getOrDefault("config_version", -1) == 1,
                "a non-numeric config_version should parse to 0 and migrate to 1 without crashing");
        } catch (IOException e) {
            throw new RuntimeException("migrateConfigParsePaths (a) I/O failed", e);
        } finally {
            deleteQuietly(tmpA);
        }

        Path tmpB = configDir().resolve("et-migparse-noversion.properties");
        try {
            Files.writeString(tmpB, "test_no_version_marker=false\nstale_thing=x\n");

            boolean bundledPresent = getClass().getClassLoader().getResourceAsStream(NOVER_BUNDLED) != null;
            ADConfig cfgB = new ADConfig(EnchantTweaker.MOD_NAME, tmpB.getFileName().toString(), NOVER_BUNDLED);

            helper.assertTrue(cfgB.getOrDefault("config_version", -99) == -99,
                "a versionless bundled resource should disable the version machinery (no config_version stamp)");
            if (bundledPresent) {
                helper.assertTrue(cfgB.getOrDefault("another_test_key", -1) == 42,
                    "key-diff should add another_test_key from the versionless bundled resource");
            }
        } catch (IOException e) {
            throw new RuntimeException("migrateConfigParsePaths (b) I/O failed", e);
        } finally {
            deleteQuietly(tmpB);
        }
        helper.complete();
    }

    // schema immutability contracts

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaImmutability(TestContext helper) {
        boolean threw = false;
        try {
            ETConfigSchema.defaults().put("junk_key", "junk_value");
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        helper.assertTrue(threw, "defaults() should return an unmodifiable map");

        List<String> a = ETConfigSchema.keysIn("anvil_tweaks");
        List<String> b = ETConfigSchema.keysIn("anvil_tweaks");
        helper.assertTrue(a != b, "keysIn() should return distinct list instances per call");
        a.add("junk_key");
        helper.assertTrue(!b.contains("junk_key"), "mutating one keysIn() result must not affect another");
        helper.assertTrue(!ETConfigSchema.keysIn("anvil_tweaks").contains("junk_key"),
            "mutating a keysIn() result must not affect the schema's internal state");
        helper.complete();
    }

    // schema minor fallbacks

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaFallbacks(TestContext helper) {
        helper.assertTrue(ETConfigSchema.expected("not_a_real_key").isEmpty(),
            "expected() for an unknown key should be empty");
        helper.assertTrue(ETConfigSchema.categoryOf("not_a_real_key") == null,
            "categoryOf() for an unknown key should be null");
        helper.assertTrue("internal".equals(ETConfigSchema.categoryOf("config_version")),
            "categoryOf(config_version) should be 'internal' even though config_version is reserved");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void schemaLiveConfigParity(TestContext helper) {
        // load a fresh, unmigrated config from a verbatim copy of the bundled defaults
        // (a verbatim
        // copy means no key-diff rewrite runs, so getKeys() is exactly the parsed key
        // set)
        Path tmp = configDir().resolve("et-parity-test.properties");
        try {
            String bundled = readResourceText(REAL_BUNDLED);
            helper.assertTrue(bundled != null, "bundled defaults resource must be present");
            Files.writeString(tmp, bundled);

            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), REAL_BUNDLED);
            Set<String> liveKeys = new HashSet<>(cfg.getKeys());

            // direction A: no config key is missing from the schema (incl. reserved
            // config_version)
            for (String key : liveKeys) {
                helper.assertTrue(ETConfigSchema.typeOf(key) != null,
                    "config key '" + key + "' has no ETConfigSchema entry (orphan config key)");
            }
            // direction B: no non-reserved schema key is missing from the loaded config
            for (String key : ETConfigSchema.defaults().keySet()) {
                helper.assertTrue(liveKeys.contains(key),
                    "schema key '" + key + "' is missing from the bundled config (orphan schema key)");
            }
            // the one reserved key is present in both the schema and the loaded config
            helper.assertTrue(ETConfigSchema.typeOf("config_version") != null,
                "reserved config_version must have a schema type");
            helper.assertTrue(liveKeys.contains("config_version"),
                "reserved config_version must be present in the loaded config");
        } catch (IOException e) {
            throw new RuntimeException("schemaLiveConfigParity I/O failed", e);
        } finally {
            deleteQuietly(tmp);
        }

        // every mixin's config key resolves to a real schema entry (no orphan mixin
        // key)
        for (Map.Entry<String, String> entry : ETMixinPlugin.getMixinKeys().entrySet()) {
            String cfgKey = entry.getValue();
            helper.assertTrue(ETConfigSchema.typeOf(cfgKey) != null,
                "mixin '" + entry.getKey() + "' maps to config key '" + cfgKey + "' which has no schema entry");
        }
        // every client-local cosmetic key exists in the schema
        for (String cfgKey : ETMixinPlugin.getClientLocalKeys()) {
            helper.assertTrue(ETConfigSchema.typeOf(cfgKey) != null,
                "CLIENT_LOCAL_KEY '" + cfgKey + "' has no schema entry");
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void parseRobustness(TestContext helper) {
        Path tmp = configDir().resolve("et-parse-test.properties");
        try {
            String fixture = String.join("\r\n", "# comment=should_be_ignored", // '#' line -> comment, no key
                "!bang=hello", // '!' is NOT a comment in ADConfig -> key "!bang"
                "", // blank line -> skipped
                "     ", // whitespace-only line -> skipped
                "Cheap_Names=TRUE", // key + value both lowercased
                "dup_key=first", // duplicate: first
                "dup_key=second", // ...last write wins
                "  spaced  =  padded_val  ", // key/value trimmed
                "value_with_eq=a=b=c", // split on first '=' only
                "empty_val=", // key present, empty value
                "missing_eq_line"); // no '=' -> syntax error, skipped
            Files.writeString(tmp, fixture);

            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(),
                "enchanttweaker-test/NONEXISTENT-defaults.properties");
            List<String> keys = cfg.getKeys();

            // '#' comment produced no key; '!' line DID produce a key (ADConfig only treats
            // '#' as a comment)
            helper.assertTrue(keys.stream().noneMatch(k -> k.contains("comment")), "'#' line must not create any key");
            helper.assertTrue(keys.contains("!bang"), "'!' is not a comment: '!bang=hello' should create key '!bang'");
            helper.assertTrue("hello".equals(cfg.getOrDefault("!bang", "x")), "'!bang' should hold value 'hello'");

            // key and value are both lowercased on parse
            helper.assertTrue(keys.contains("cheap_names"), "key should be lowercased to cheap_names");
            helper.assertTrue(cfg.getOrDefault("cheap_names", false),
                "lowercased value TRUE->true should parse boolean true");

            // duplicate key: last write wins
            helper.assertTrue("second".equals(cfg.getOrDefault("dup_key", "x")),
                "duplicate key should keep the last value (got " + cfg.getOrDefault("dup_key", "x") + ")");

            // surrounding whitespace trimmed from both key and value (no stray \r either)
            helper.assertTrue(keys.contains("spaced"), "whitespace around key should be trimmed");
            helper.assertTrue("padded_val".equals(cfg.getOrDefault("spaced", "x")),
                "whitespace around value should be trimmed (got '" + cfg.getOrDefault("spaced", "x") + "')");

            // value containing '=' is preserved (split limit 2)
            helper.assertTrue("a=b=c".equals(cfg.getOrDefault("value_with_eq", "x")),
                "value with '=' should be preserved (got '" + cfg.getOrDefault("value_with_eq", "x") + "')");

            // key with empty value is stored as the empty string
            helper.assertTrue(keys.contains("empty_val"), "empty-value key should still be stored");
            helper.assertTrue(cfg.getOrDefault("empty_val", "sentinel").isEmpty(),
                "empty-value key should hold the empty string");

            // line without '=' is skipped entirely
            helper.assertTrue(!keys.contains("missing_eq_line"), "line without '=' should be skipped");
        } catch (IOException e) {
            throw new RuntimeException("parseRobustness I/O failed", e);
        } finally {
            deleteQuietly(tmp);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void getOrDefaultCoercion(TestContext helper) {
        Path tmp = configDir().resolve("et-coerce-test.properties");
        try {
            Files.writeString(tmp, "");
            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(),
                "enchanttweaker-test/NONEXISTENT-defaults.properties");

            Map<String, String> junk = new HashMap<>();
            junk.put("int_empty", "");
            junk.put("int_bad", "abc");
            junk.put("int_over", "99999999999999"); // out of int range
            junk.put("int_ok", "42");
            junk.put("dbl_bad", "xyz");
            junk.put("dbl_over", "1e400"); // parses to Infinity and must be rejected
            junk.put("dbl_nan", "NaN");
            junk.put("dbl_ok", "3.5");
            junk.put("flt_over", "1e50");
            junk.put("flt_nan", "NaN");
            junk.put("flt_ok", "1.25");
            junk.put("bool_junk", "yes");
            junk.put("bool_empty", "");
            junk.put("bool_true", "true");
            cfg.setAll(junk);

            // int: empty / non-numeric / overflow all fall back to the default; a clean
            // value parses
            helper.assertTrue(cfg.getOrDefault("int_empty", 7) == 7, "empty int value should fall back to default");
            helper.assertTrue(cfg.getOrDefault("int_bad", 7) == 7, "non-numeric int value should fall back to default");
            helper.assertTrue(cfg.getOrDefault("int_over", 7) == 7,
                "overflowing int value should fall back to default");
            helper.assertTrue(cfg.getOrDefault("int_ok", 7) == 42, "clean int value should parse");

            // floating-point getters accept finite values and reject non-numeric or
            // non-finite values
            helper.assertTrue(cfg.getOrDefault("dbl_bad", 1.5) == 1.5,
                "non-numeric double value should fall back to default");
            helper.assertTrue(cfg.getOrDefault("dbl_ok", 1.5) == 3.5, "clean double value should parse");
            helper.assertTrue(cfg.getOrDefault("dbl_over", 1.5) == 1.5,
                "overflowing double should fall back instead of returning Infinity");
            helper.assertTrue(cfg.getOrDefault("dbl_nan", 1.5) == 1.5, "NaN double should fall back to the default");
            helper.assertTrue(cfg.getOrDefault("flt_over", 2.5f) == 2.5f,
                "overflowing float should fall back instead of returning Infinity");
            helper.assertTrue(cfg.getOrDefault("flt_nan", 2.5f) == 2.5f, "NaN float should fall back to the default");
            helper.assertTrue(cfg.getOrDefault("flt_ok", 2.5f) == 1.25f, "clean float value should parse");

            // invalid present booleans preserve the supplied default
            helper.assertTrue(cfg.getOrDefault("bool_junk", true),
                "a present invalid boolean should fall back to the true default");
            helper.assertTrue(cfg.getOrDefault("bool_empty", true),
                "an empty boolean should fall back to the true default");
            helper.assertTrue(cfg.getOrDefault("bool_true", false), "value 'true' parses to boolean true");

            // missing key / null key both return the supplied default
            helper.assertTrue(cfg.getOrDefault("no_such_key", 99) == 99, "missing key should return the default");
            helper.assertTrue(cfg.getOrDefault(null, 99) == 99, "null key should return the default");
            helper.assertTrue("d".equals(cfg.getOrDefault("no_such_key", "d")),
                "missing key (String) should return the default");
        } catch (IOException e) {
            throw new RuntimeException("getOrDefaultCoercion I/O failed", e);
        } finally {
            deleteQuietly(tmp);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void migrationMissingVersionAppends(TestContext helper) {
        Path tmp = configDir().resolve("et-mignover-test.properties");
        try {
            String bundled = readResourceText(REAL_BUNDLED);
            helper.assertTrue(bundled != null, "bundled defaults resource must be present");
            // drop the config_version line entirely (not just set it to 0)
            String noVersion = String.join("\n",
                bundled.lines().filter(l -> !l.trim().toLowerCase().startsWith("config_version=")).toList());
            helper.assertTrue(!noVersion.contains("config_version="), "fixture should have no config_version line");
            Files.writeString(tmp, noVersion);

            ADConfig cfg = new ADConfig(EnchantTweaker.MOD_NAME, tmp.getFileName().toString(), REAL_BUNDLED);

            helper.assertTrue(cfg.getOrDefault("config_version", -1) == 1,
                "a config missing config_version should be bumped to the bundled version 1");
            String fileText = Files.readString(tmp);
            helper.assertTrue(fileText.contains("config_version=1"),
                "the bump should be persisted to disk even though the line was absent (append path)");
        } catch (IOException e) {
            throw new RuntimeException("migrationMissingVersionAppends I/O failed", e);
        } finally {
            deleteQuietly(tmp);
        }
        helper.complete();
    }

    // isolated-migration helpers

    private static final String REAL_BUNDLED = "assets/" + EnchantTweaker.MOD_ID + "/enchant-tweaker.properties";
    private static final String NOVER_BUNDLED = "enchanttweaker-test/defaults-no-version.properties";

    private static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    private String readResourceText(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("failed to read resource " + path, e);
        }
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
