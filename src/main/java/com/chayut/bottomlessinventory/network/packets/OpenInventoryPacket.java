package com.chayut.bottomlessinventory.network.packets;

import com.chayut.bottomlessinventory.network.BottomlessNetworking;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

/**
 * Packet sent from client to server to request opening the bottomless inventory screen.
 * This packet has no payload - it's just a simple signal.
 */
public record OpenInventoryPacket() implements CustomPacketPayload {

    // Packet type identifier
    public static final CustomPacketPayload.Type<OpenInventoryPacket> TYPE =
            new CustomPacketPayload.Type<>(BottomlessNetworking.OPEN_INVENTORY_ID);

    // Codec for serialization/deserialization
    public static final StreamCodec<ByteBuf, OpenInventoryPacket> CODEC =
            StreamCodec.unit(new OpenInventoryPacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
