package com.adibarra.enchanttweaker.test;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.network.ConfigSyncPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.HashMap;
import java.util.Map;

public class PayloadGameTest implements FabricGameTest {

    /** fresh registry-aware buffer, per the CODEC's RegistryByteBuf contract */
    private static RegistryByteBuf newBuf(TestContext helper) {
        return new RegistryByteBuf(Unpooled.buffer(), helper.getWorld().getRegistryManager());
    }

    // round-trip encode/decode

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void payloadRoundTrip(TestContext helper) {
        Map<String, String> sample = Map.of(
            "cheap_names", "true",
            "nte_max_cost", "100",
            "disable_enchantments", "sharpness,mending"
        );

        RegistryByteBuf buf = newBuf(helper);
        ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(sample));
        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.version() == ConfigSyncPayload.PROTOCOL_VERSION,
            "decoded version should equal PROTOCOL_VERSION");
        helper.assertTrue(decoded.configData().equals(sample),
            "decoded map should equal the encoded map");
        helper.complete();
    }

    // version mismatch is consumed and ignored

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void payloadVersionMismatchGraceful(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        buf.writeVarInt(999);
        // a few junk bytes standing in for an unrecognized payload body
        buf.writeVarInt(42);
        buf.writeString("junk");

        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.version() == 999, "mismatched version should be preserved");
        helper.assertTrue(decoded.configData().isEmpty(), "mismatched payload should yield an empty map");
        helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed on version mismatch");
        helper.complete();
    }

    // convenience constructor stamps PROTOCOL_VERSION

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void payloadConvenienceConstructor(TestContext helper) {
        ConfigSyncPayload payload = new ConfigSyncPayload(Map.of("cheap_names", "true"));
        helper.assertTrue(payload.version() == ConfigSyncPayload.PROTOCOL_VERSION,
            "convenience constructor should stamp PROTOCOL_VERSION");
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void getConfigMapStripsClientLocalKeys(TestContext helper) {
        ETTestHelper.setConfigValue("roman_numerals", "true");
        ETTestHelper.setConfigValue("shiny_name", "true");
        ETTestHelper.setConfigValue("cheap_names", "true");
        try {
            Map<String, String> map = ETMixinPlugin.getConfigMap();

            helper.assertFalse(map.containsKey("roman_numerals"),
                "getConfigMap must strip client-local key roman_numerals");
            helper.assertFalse(map.containsKey("shiny_name"),
                "getConfigMap must strip client-local key shiny_name");
            helper.assertTrue(map.containsKey("cheap_names"),
                "getConfigMap must retain server key cheap_names");
            helper.assertTrue("true".equals(map.get("cheap_names")),
                "cheap_names should carry the value we set (got " + map.get("cheap_names") + ")");
            helper.assertTrue(map.containsKey("mod_enabled"),
                "getConfigMap must retain server key mod_enabled");
        } finally {
            ETTestHelper.setConfigValue("roman_numerals", "true");
            ETTestHelper.setConfigValue("shiny_name", "false");
            ETTestHelper.setConfigValue("cheap_names", "false");
        }
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void syncConfigFromAppliesAndClearsFeatureCache(TestContext helper) {
        ETTestHelper.setConfigValue("mod_enabled", "true");
        ETTestHelper.setConfigValue("cheap_names", "false");
        try {
            // prime the cache with cheap_names gated OFF
            helper.assertFalse(ETMixinPlugin.getMixinConfig("CheapNamesMixin"),
                "cheap_names mixin should be gated off before the sync");

            ETMixinPlugin.syncConfigFrom(Map.of("cheap_names", "true"));

            helper.assertTrue(ETMixinPlugin.getMixinConfig("CheapNamesMixin"),
                "syncConfigFrom must apply cheap_names=true AND clear the feature cache");
        } finally {
            ETMixinPlugin.syncConfigFrom(Map.of("cheap_names", "false"));
            ETTestHelper.setConfigValue("mod_enabled", "true");
        }
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void realWirePayloadRoundTrip(TestContext helper) {
        ETTestHelper.setConfigValue("roman_numerals", "true");
        ETTestHelper.setConfigValue("shiny_name", "true");
        try {
            Map<String, String> real = ETMixinPlugin.getConfigMap();

            // the real map carries the empty-string disable_enchantments value and the
            // full config surface (~140 keys minus the two stripped client-local ones)
            helper.assertTrue(real.containsKey("disable_enchantments"),
                "real config map should contain disable_enchantments");
            helper.assertTrue(real.get("disable_enchantments").isEmpty(),
                "disable_enchantments default is the empty string (got '" + real.get("disable_enchantments") + "')");
            helper.assertTrue(real.size() > 100,
                "real config map should carry the full config surface (got " + real.size() + " entries)");

            RegistryByteBuf buf = newBuf(helper);
            ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(real));
            ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

            helper.assertTrue(decoded.configData().equals(real),
                "decoded ~140-entry map should equal the source map (incl. empty-string values)");
            helper.assertTrue(buf.readableBytes() == 0,
                "buffer must be fully consumed after the real round-trip");
            helper.assertFalse(decoded.configData().containsKey("roman_numerals"),
                "wire payload must not carry client-local roman_numerals");
            helper.assertFalse(decoded.configData().containsKey("shiny_name"),
                "wire payload must not carry client-local shiny_name");
        } finally {
            ETTestHelper.setConfigValue("roman_numerals", "true");
            ETTestHelper.setConfigValue("shiny_name", "false");
        }
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void encoderWritesRecordVersion(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(2, Map.of("cheap_names", "true")));
        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.version() == 2,
            "encoder must write value.version() (2), so decode reads version 2 (got " + decoded.version() + ")");
        helper.assertTrue(decoded.configData().isEmpty(),
            "version 2 != PROTOCOL_VERSION so the mismatch branch must yield an empty map");
        helper.assertTrue(buf.readableBytes() == 0,
            "the mismatch branch must fully consume the buffer");
        helper.complete();
    }

    // syncConfigFrom clears CAPMOD_CACHE too
    // prime the capmod cache with cap 5, sync sharpness=3, and confirm the new cap is read:
    // this fails unless syncConfigFrom clears CAPMOD_CACHE

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void syncConfigFromClearsCapmodCache(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 5);
        try {
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 5,
                "primed capmod level should be 5");

            ETMixinPlugin.syncConfigFrom(Map.of("sharpness", "3"));

            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 3,
                "syncConfigFrom must clear CAPMOD_CACHE so the synced cap 3 is read (got "
                    + ETMixinPlugin.getCapmodLevel("sharpness", 10) + ")");
        } finally {
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    // restoreLocalConfig discards synced values, reloads disk
    // sync an override, then restoreLocalConfig() must reload the on-disk baseline
    // (cheap_names=false), discarding the synced true

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void restoreLocalConfigReloadsDisk(TestContext helper) {
        try {
            ETMixinPlugin.syncConfigFrom(Map.of("cheap_names", "true"));
            helper.assertTrue(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "synced cheap_names should be true in memory before the restore");

            ETMixinPlugin.restoreLocalConfig();

            helper.assertFalse(ETMixinPlugin.getConfig().getOrDefault("cheap_names", false),
                "restoreLocalConfig must reload the on-disk default (cheap_names=false)");
        } finally {
            // `restoreLocalConfig` already reloaded the pristine disk state; re-run to be certain
            ETMixinPlugin.restoreLocalConfig();
        }
        helper.complete();
    }

    // empty-map round trip

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void emptyMapRoundTrip(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(Map.of()));
        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.configData().isEmpty(),
            "empty-map payload should decode to an empty map");
        helper.assertTrue(decoded.version() == ConfigSyncPayload.PROTOCOL_VERSION,
            "empty-map payload uses the convenience ctor's PROTOCOL_VERSION");
        helper.assertTrue(buf.readableBytes() == 0,
            "buffer must be fully consumed for the empty map");
        helper.complete();
    }

    // decode robustness: duplicate key, last write wins

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void decodeDuplicateKeyLastWins(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        buf.writeVarInt(ConfigSyncPayload.PROTOCOL_VERSION);
        buf.writeVarInt(2);                 // two pairs, same key
        buf.writeString("cheap_names");
        buf.writeString("false");
        buf.writeString("cheap_names");
        buf.writeString("true");

        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.configData().size() == 1,
            "a duplicate key should collapse to a single map entry (got " + decoded.configData().size() + ")");
        helper.assertTrue("true".equals(decoded.configData().get("cheap_names")),
            "last write should win for a duplicate key (got " + decoded.configData().get("cheap_names") + ")");
        helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed");
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void decodeTruncatedThrows(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        buf.writeVarInt(ConfigSyncPayload.PROTOCOL_VERSION);
        buf.writeVarInt(5);                 // claims five pairs
        buf.writeString("cheap_names");     // ...but supplies only one
        buf.writeString("true");

        boolean threw = false;
        try {
            ConfigSyncPayload.CODEC.decode(buf);
        } catch (Exception e) {
            threw = true;
        }
        helper.assertTrue(threw,
            "a truncated payload (size=5, one pair supplied) must throw on decode (EOF / disconnect branch)");
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void payloadUnicodeAndEmptyValues(TestContext helper) {
        Map<String, String> sample = new HashMap<>();
        sample.put("accented", "café");
        sample.put("cjk", "日本語");
        sample.put("emoji", "🗡️⚗️");
        sample.put("empty_value", "");
        sample.put("unicode_key_日", "value");

        RegistryByteBuf buf = newBuf(helper);
        ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(sample));
        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.version() == ConfigSyncPayload.PROTOCOL_VERSION,
            "decoded version should equal PROTOCOL_VERSION");
        helper.assertTrue(decoded.configData().equals(sample),
            "unicode/empty-string keys and values must round-trip exactly");
        helper.assertTrue("".equals(decoded.configData().get("empty_value")),
            "empty-string value must survive as the empty string");
        helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed");
        helper.complete();
    }

    // very large map round-trip exercises multi-byte varint size
    // a 2000-entry map forces a multi-byte writeVarInt(size) and a large body; decode must
    // reconstruct every pair and leave no trailing bytes

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void payloadLargeMap(TestContext helper) {
        Map<String, String> big = new HashMap<>();
        for (int i = 0; i < 2000; i++) {
            big.put("key_" + i, "value_" + i);
        }

        RegistryByteBuf buf = newBuf(helper);
        ConfigSyncPayload.CODEC.encode(buf, new ConfigSyncPayload(big));
        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.configData().size() == 2000,
            "large map should decode all 2000 entries (got " + decoded.configData().size() + ")");
        helper.assertTrue(decoded.configData().equals(big),
            "large map should round-trip exactly");
        helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed after the large round-trip");
        helper.complete();
    }


    @GameTest(templateName = EMPTY_STRUCTURE)
    public void payloadVersionMismatchEmptyBody(TestContext helper) {
        RegistryByteBuf buf = newBuf(helper);
        buf.writeVarInt(7);   // bogus version, nothing after it

        ConfigSyncPayload decoded = ConfigSyncPayload.CODEC.decode(buf);

        helper.assertTrue(decoded.version() == 7, "mismatched version should be preserved");
        helper.assertTrue(decoded.configData().isEmpty(),
            "a mismatched version with no body must still yield an empty map");
        helper.assertTrue(buf.readableBytes() == 0, "buffer must be fully consumed even with an empty body");
        helper.complete();
    }
}
