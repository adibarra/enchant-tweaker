package com.adibarra.enchanttweaker.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConfigSyncPayload(int version, Map<String, String> configData) implements CustomPayload {

    public ConfigSyncPayload {
        Objects.requireNonNull(configData, "configData");
        configData.forEach((key, value) -> {
            Objects.requireNonNull(key, "configData key");
            Objects.requireNonNull(value, "configData value");
        });
        configData = Map.copyOf(configData);
    }

    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_CONFIG_ENTRIES = 256;

    public static final CustomPayload.Id<ConfigSyncPayload> ID = new CustomPayload.Id<>(
        Identifier.of("enchanttweaker", "config_sync_v" + PROTOCOL_VERSION));

    /** stamps payloads with the protocol version */
    public ConfigSyncPayload(Map<String, String> configData) {
        this(PROTOCOL_VERSION, configData);
    }

    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC = PacketCodec.of((value, buf) -> {
        if (value.configData().size() > MAX_CONFIG_ENTRIES)
            throw new IllegalArgumentException("Config sync payload has too many entries");
        buf.writeVarInt(value.version());
        buf.writeVarInt(value.configData().size());
        value.configData().forEach((key, val) -> {
            buf.writeString(key);
            buf.writeString(val);
        });
    }, buf -> {
        // discard unknown protocol versions after consuming the payload
        int version = buf.readVarInt();
        if (version != PROTOCOL_VERSION) {
            // consume the rest so Fabric does not disconnect
            buf.skipBytes(buf.readableBytes());
            return new ConfigSyncPayload(version, Map.of());
        }
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_CONFIG_ENTRIES || size > buf.readableBytes() / 2)
            throw new DecoderException("Invalid config sync entry count: " + size);
        Map<String, String> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = Objects.requireNonNull(buf.readString(), "config sync key");
            String value = Objects.requireNonNull(buf.readString(), "config sync value");
            if (map.containsKey(key))
                throw new DecoderException("Duplicate config sync key: " + key);
            map.put(key, value);
        }
        if (buf.readableBytes() != 0)
            throw new DecoderException("Trailing bytes in config sync payload: " + buf.readableBytes());
        return new ConfigSyncPayload(version, map);
    });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
