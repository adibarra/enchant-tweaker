package com.adibarra.enchanttweaker.test;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.network.ConfigSyncPayload;

public class PayloadGameTest implements FabricGameTest {

    /** creates a registry-aware buffer for CODEC operations */
    private static RegistryByteBuf newBuf(TestContext helper) {
        return new RegistryByteBuf(Unpooled.buffer(), helper.getWorld().getRegistryManager());
    }

    private static void release(RegistryByteBuf buf) {
        buf.release();
    }

    // round-trip encode/decode

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadRoundTrip(TestContext helper) {
        Map<String, String> sample = Map.of("cheap_names", "true", "nte_max_cost", "100", "disable_enchantments",
            "sharpness,mending");

        RegistryByteBuf buf = newBuf(helper);
        try {
            ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(sample));
            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.version() == ConfigSyncPayload.PROTOCOL_VERSION,
                "decoded version should equal PROTOCOL_VERSION");
            helper.assertTrue(decoded.configData().equals(sample), "decoded map should equal the encoded map");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    // version mismatch is consumed and ignored

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadVersionMismatchGraceful(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            int mismatchedVersion = ConfigSyncPayload.PROTOCOL_VERSION + 1;
            buf.writeVarInt(mismatchedVersion);
            // a few junk bytes standing in for an unrecognized payload
            buf.writeVarInt(42);
            buf.writeString("junk");

            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.version() == mismatchedVersion, "mismatched version should be preserved");
            helper.assertTrue(decoded.configData().isEmpty(), "mismatched payload should yield an empty map");
            helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed on version mismatch");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    // convenience constructor stamps PROTOCOL_VERSION

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadConvenienceConstructor(TestContext helper) {
        ConfigSyncPayload payload = new ConfigSyncPayload(Map.of("cheap_names", "true"));
        helper.assertTrue(payload.version() == ConfigSyncPayload.PROTOCOL_VERSION,
            "convenience constructor should stamp PROTOCOL_VERSION");
        helper.assertTrue(ConfigSyncPayload.ID.id().getPath().endsWith("_v" + ConfigSyncPayload.PROTOCOL_VERSION),
            "payload ID should encode the wire protocol version");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadRejectsNullMap(TestContext helper) {
        boolean rejected = false;
        try {
            new ConfigSyncPayload((Map<String, String>) null);
        } catch (NullPointerException e) {
            rejected = true;
        }
        helper.assertTrue(rejected, "payload construction must reject a null config map");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadRejectsNullEntries(TestContext helper) {
        Map<String, String> nullKey = new HashMap<>();
        nullKey.put(null, "true");
        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("cheap_names", null);

        boolean nullKeyRejected = false;
        try {
            new ConfigSyncPayload(nullKey);
        } catch (NullPointerException e) {
            nullKeyRejected = true;
        }
        boolean nullValueRejected = false;
        try {
            new ConfigSyncPayload(nullValue);
        } catch (NullPointerException e) {
            nullValueRejected = true;
        }

        helper.assertTrue(nullKeyRejected, "payload construction must reject null config keys");
        helper.assertTrue(nullValueRejected, "payload construction must reject null config values");
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadSnapshotsConfigMap(TestContext helper) {
        Map<String, String> source = new HashMap<>();
        source.put("cheap_names", "true");

        ConfigSyncPayload payload = new ConfigSyncPayload(source);
        source.put("cheap_names", "false");
        source.put("new_key", "new_value");

        helper.assertTrue(payload.configData().size() == 1, "payload must retain the map size from construction");
        helper.assertTrue("true".equals(payload.configData().get("cheap_names")),
            "payload must retain values from construction");
        helper.assertFalse(payload.configData().containsKey("new_key"),
            "caller mutations must not add entries to the payload");

        boolean accessorRejected = false;
        try {
            payload.configData().put("another_key", "another_value");
        } catch (UnsupportedOperationException e) {
            accessorRejected = true;
        }
        helper.assertTrue(accessorRejected, "payload config data must be immutable");
        Map<String, String> oversized = new HashMap<>();
        for (int i = 0; i <= ConfigSyncPayload.MAX_CONFIG_ENTRIES; i++)
            oversized.put("key_" + i, "value_" + i);
        ConfigSyncPayload oversizedPayload = new ConfigSyncPayload(oversized);
        oversized.clear();

        RegistryByteBuf buf = newBuf(helper);
        try {
            boolean rejected = false;
            try {
                ConfigSyncPayload.CODEC.encode(buf, oversizedPayload);
            } catch (IllegalArgumentException e) {
                rejected = true;
            }
            helper.assertTrue(rejected, "snapshot must preserve entry limits after caller mutation");
            helper.assertTrue(buf.readableBytes() == 0,
                "oversized snapshot must be rejected before writing a partial payload");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void getConfigMapStripsClientLocalKeys(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("roman_numerals", "shiny_name", "cheap_names");
        try {
            ETTestHelper.setConfigValue("roman_numerals", "true");
            ETTestHelper.setConfigValue("shiny_name", "true");
            ETTestHelper.setConfigValue("cheap_names", "true");
            Map<String, String> map = ETMixinPlugin.getConfigMap();

            helper.assertFalse(map.containsKey("roman_numerals"),
                "getConfigMap must strip client-local key roman_numerals");
            helper.assertFalse(map.containsKey("shiny_name"), "getConfigMap must strip client-local key shiny_name");
            helper.assertTrue(map.containsKey("cheap_names"), "getConfigMap must retain server key cheap_names");
            helper.assertTrue("true".equals(map.get("cheap_names")),
                "cheap_names should carry the value we set (got " + map.get("cheap_names") + ")");
            helper.assertTrue(map.containsKey("mod_enabled"), "getConfigMap must retain server key mod_enabled");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void syncConfigFromAppliesAndClearsFeatureCache(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("mod_enabled", "cheap_names");
        try {
            ETTestHelper.setConfigValue("mod_enabled", "true");
            ETTestHelper.setConfigValue("cheap_names", "false");
            // prime the cache with cheap_names gated OFF
            helper.assertFalse(ETMixinPlugin.getMixinConfig("CheapNamesMixin"),
                "cheap_names mixin should be gated off before the sync");

            ETMixinPlugin.syncConfigFrom(Map.of("cheap_names", "true"));

            helper.assertTrue(ETMixinPlugin.getMixinConfig("CheapNamesMixin"),
                "syncConfigFrom must apply cheap_names=true AND clear the feature cache");
        } finally {
            ETMixinPlugin.restoreLocalConfig();
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void syncConfigFromPreservesClientLocalKeys(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("roman_numerals", "shiny_name", "cheap_names");
        try {
            ETTestHelper.setConfigValue("roman_numerals", "false");
            ETTestHelper.setConfigValue("shiny_name", "false");
            ETTestHelper.setConfigValue("cheap_names", "false");
            ETMixinPlugin.syncConfigFrom(Map.of("roman_numerals", "true", "shiny_name", "true", "cheap_names", "true"));
            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("roman_numerals", true),
                "server sync must not overwrite roman_numerals");
            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("shiny_name", true),
                "server sync must not overwrite shiny_name");
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "server sync should apply cheap_names");
        } finally {
            ETMixinPlugin.restoreLocalConfig();
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void realWirePayloadRoundTrip(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("roman_numerals", "shiny_name");
        try {
            ETTestHelper.setConfigValue("roman_numerals", "true");
            ETTestHelper.setConfigValue("shiny_name", "true");
            Map<String, String> real = ETMixinPlugin.getConfigMap();

            // real maps retain empty disable_enchantments values
            // client-local keys are excluded from transmitted configurations
            helper.assertTrue(real.containsKey("disable_enchantments"),
                "real config map should contain disable_enchantments");
            helper.assertTrue(real.get("disable_enchantments").isEmpty(),
                "disable_enchantments default is the empty string (got '" + real.get("disable_enchantments") + "')");
            helper.assertTrue(real.size() > 100,
                "real config map should carry the full config surface (got " + real.size() + " entries)");

            RegistryByteBuf buf = newBuf(helper);
            try {
                ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(real));
                ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

                helper.assertTrue(decoded.configData().equals(real),
                    "decoded ~140-entry map should equal the source map (incl. empty-string values)");
                helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed after the real round-trip");
                helper.assertFalse(decoded.configData().containsKey("roman_numerals"),
                    "wire payload must not carry client-local roman_numerals");
                helper.assertFalse(decoded.configData().containsKey("shiny_name"),
                    "wire payload must not carry client-local shiny_name");
            } finally {
                release(buf);
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void encoderWritesRecordVersion(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            int mismatchedVersion = ConfigSyncPayload.PROTOCOL_VERSION + 1;
            ConfigSyncPayload.CODEC.encode(buf,
                new ConfigSyncPayload(mismatchedVersion, Map.of("cheap_names", "true")));
            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.version() == mismatchedVersion,
                "encoder must write value.version() so decode reads the mismatched version (got " + decoded.version()
                    + ")");
            helper.assertTrue(decoded.configData().isEmpty(), "mismatched payload must yield an empty map");
            helper.assertTrue(buf.readableBytes() == 0, "the mismatch branch must fully consume the buffer");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    // syncConfigFrom clears the capmod cache
    // synced sharpness caps replace cached caps

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void syncConfigFromClearsCapmodCache(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        try {
            ETTestHelper.setCapmod(true);
            ETTestHelper.setEnchantCap("sharpness", 5);
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 5, "primed capmod level should be 5");

            ETMixinPlugin.syncConfigFrom(Map.of("sharpness", "3"));

            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 3,
                "syncConfigFrom must clear CAPMOD_CACHE so the synced cap 3 is read (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");
        } finally {
            ETMixinPlugin.restoreLocalConfig();
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // restoreLocalConfig discards synced values and reloads disk
    // cheap_names reverts to the disk default

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void restoreLocalConfigReloadsDisk(TestContext helper) {
        try {
            ETMixinPlugin.syncConfigFrom(Map.of("cheap_names", "true"));
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "synced cheap_names should be true in memory before the restore");

            ETMixinPlugin.restoreLocalConfig();

            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "restoreLocalConfig must reload the on-disk default (cheap_names=false)");
        } finally {
            // restore the pristine disk configuration
            ETMixinPlugin.restoreLocalConfig();
        }
        helper.complete();
    }

    // empty-map round trip

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void emptyMapRoundTrip(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(Map.of()));
            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.configData().isEmpty(), "empty-map payload should decode to an empty map");
            helper.assertTrue(decoded.version() == ConfigSyncPayload.PROTOCOL_VERSION,
                "empty-map payload uses the convenience ctor's PROTOCOL_VERSION");
            helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed for the empty map");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void decodeRejectsTrailingBytes(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            buf.writeVarInt(ConfigSyncPayload.PROTOCOL_VERSION);
            buf.writeVarInt(1);
            buf.writeString("cheap_names");
            buf.writeString("true");
            buf.writeByte(0x7F);

            boolean rejected = false;
            try {
                ConfigSyncPayload.CODEC.decode(buf);
            } catch (DecoderException e) {
                rejected = true;
            }
            helper.assertTrue(rejected, "known-version payloads must reject trailing bytes");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    // decode robustness: duplicate keys are rejected

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void decodeRejectsDuplicateKey(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            buf.writeVarInt(ConfigSyncPayload.PROTOCOL_VERSION);
            buf.writeVarInt(2); // two pairs use the same key
            buf.writeString("cheap_names");
            buf.writeString("false");
            buf.writeString("cheap_names");
            buf.writeString("true");

            boolean rejected = false;
            try {
                ConfigSyncPayload.CODEC.decode(buf);
            } catch (DecoderException e) {
                rejected = true;
            }

            helper.assertTrue(rejected, "known-version payloads must reject duplicate keys");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void decodeTruncatedThrows(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            buf.writeVarInt(ConfigSyncPayload.PROTOCOL_VERSION);
            buf.writeVarInt(5); // claims five pairs
            buf.writeString("cheap_names"); // supplies one pair
            buf.writeString("true");

            boolean truncated = false;
            try {
                ConfigSyncPayload.CODEC.decode(buf);
            } catch (IndexOutOfBoundsException e) {
                truncated = true;
            }
            helper.assertTrue(truncated, "a truncated payload (size=5, one pair supplied) must throw EOF on decode");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void decodeRejectsExcessiveEntryCount(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            buf.writeVarInt(ConfigSyncPayload.PROTOCOL_VERSION);
            buf.writeVarInt(ConfigSyncPayload.MAX_CONFIG_ENTRIES + 1);

            boolean rejected = false;
            try {
                ConfigSyncPayload.CODEC.decode(buf);
            } catch (DecoderException e) {
                rejected = true;
            }
            helper.assertTrue(rejected, "payload decoder must reject entry counts above the protocol maximum");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void decodeRejectsNegativeEntryCount(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            buf.writeVarInt(ConfigSyncPayload.PROTOCOL_VERSION);
            buf.writeVarInt(-1);

            boolean rejected = false;
            try {
                ConfigSyncPayload.CODEC.decode(buf);
            } catch (DecoderException e) {
                rejected = true;
            }
            helper.assertTrue(rejected, "payload decoder must reject negative entry counts");
        } finally {
            release(buf);
        }
        helper.complete();
    }
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadUnicodeAndEmptyValues(TestContext helper) {
        Map<String, String> sample = new HashMap<>();
        sample.put("accented", "café");
        sample.put("cjk", "日本語");
        sample.put("emoji", "🗡️⚗️");
        sample.put("empty_value", "");
        sample.put("unicode_key_日", "value");

        RegistryByteBuf buf = newBuf(helper);
        try {
            ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(sample));
            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.version() == ConfigSyncPayload.PROTOCOL_VERSION,
                "decoded version should equal PROTOCOL_VERSION");
            helper.assertTrue(decoded.configData().equals(sample),
                "unicode/empty-string keys and values must round-trip exactly");
            helper.assertTrue("".equals(decoded.configData().get("empty_value")),
                "empty-string value must survive as the empty string");
            helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    // maximum-size map uses a multi-byte size varint
    // decoding preserves every entry and consumes all bytes

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadMaximumMap(TestContext helper) {
        Map<String, String> big = new HashMap<>();
        for (int i = 0; i < ConfigSyncPayload.MAX_CONFIG_ENTRIES; i++) {
            big.put("key_" + i, "value_" + i);
        }

        RegistryByteBuf buf = newBuf(helper);
        try {
            ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(big));
            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.configData().size() == ConfigSyncPayload.MAX_CONFIG_ENTRIES,
                "maximum map should decode every entry (got " + decoded.configData().size() + ")");
            helper.assertTrue(decoded.configData().equals(big), "maximum map should round-trip exactly");
            helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed after the maximum round-trip");
        } finally {
            release(buf);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void encoderRejectsExcessiveEntryCount(TestContext helper) {
        Map<String, String> oversized = new HashMap<>();
        for (int i = 0; i <= ConfigSyncPayload.MAX_CONFIG_ENTRIES; i++)
            oversized.put("key_" + i, "value_" + i);

        RegistryByteBuf buf = newBuf(helper);
        try {
            boolean rejected = false;
            try {
                ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(oversized));
            } catch (IllegalArgumentException e) {
                rejected = true;
            }
            helper.assertTrue(rejected, "payload encoder must reject maps above the protocol maximum");
            helper.assertTrue(buf.readableBytes() == 0,
                "encoder must reject oversized maps before writing a partial payload");
        } finally {
            release(buf);
        }
        helper.complete();
    }
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void payloadVersionMismatchEmptyBody(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        try {
            int mismatchedVersion = ConfigSyncPayload.PROTOCOL_VERSION + 1;
            buf.writeVarInt(mismatchedVersion); // bogus version, nothing after it

            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.version() == mismatchedVersion, "mismatched version should be preserved");
            helper.assertTrue(decoded.configData().isEmpty(),
                "a mismatched version with no body must still yield an empty map");
            helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed even with an empty body");
        } finally {
            release(buf);
        }
        helper.complete();
    }
}
