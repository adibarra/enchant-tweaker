package com.adibarra.enchanttweaker.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ConfigSyncPayload(int version, Map<String, String> configData) implements CustomPayload {

    public static final int PROTOCOL_VERSION = 1;

    public static final CustomPayload.Id<ConfigSyncPayload> ID =
        new CustomPayload.Id<>(Identifier.of("enchanttweaker", "config_sync"));

    /** convenience constructor that stamps the current #PROTOCOL_VERSION */
    public ConfigSyncPayload(Map<String, String> configData) {
        this(PROTOCOL_VERSION, configData);
    }

    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC =
        PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.version());
                buf.writeVarInt(value.configData.size());
                value.configData.forEach((key, val) -> {
                    buf.writeString(key);
                    buf.writeString(val);
                });
            },
            buf -> {
                // discard unknown protocol versions after consuming the payload
                int version = buf.readVarInt();
                if (version != PROTOCOL_VERSION) {
                    // consume the rest so Fabric does not disconnect
                    buf.skipBytes(buf.readableBytes());
                    return new ConfigSyncPayload(version, Map.of());
                }
                int size = buf.readVarInt();
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    map.put(buf.readString(), buf.readString());
                }
                return new ConfigSyncPayload(version, map);
            }
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
