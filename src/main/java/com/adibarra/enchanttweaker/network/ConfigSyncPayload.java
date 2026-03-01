package com.adibarra.enchanttweaker.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ConfigSyncPayload(Map<String, String> configData) implements CustomPayload {

    public static final CustomPayload.Id<ConfigSyncPayload> ID =
        new CustomPayload.Id<>(Identifier.of("enchanttweaker", "config_sync"));

    public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC =
        PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.configData.size());
                value.configData.forEach((key, val) -> {
                    buf.writeString(key);
                    buf.writeString(val);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    map.put(buf.readString(), buf.readString());
                }
                return new ConfigSyncPayload(map);
            }
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
