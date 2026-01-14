package com.chayut.bottomlessinventory.network.packets;

import com.chayut.bottomlessinventory.inventory.InfiniteInventory;
import com.chayut.bottomlessinventory.inventory.InfiniteInventoryEntry;
import com.chayut.bottomlessinventory.network.BottomlessNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client packet that syncs the infinite inventory state.
 * Supports both full syncs (entire inventory) and incremental syncs (only changed items).
 */
public record SyncInventoryPacket(
        int version,
        SyncType syncType,
        List<SyncEntry> entries
) implements CustomPacketPayload {

    /**
     * Current packet version for forward/backward compatibility.
     * Increment when packet format changes.
     */
    public static final int PACKET_VERSION = 1;

    /**
     * Types of synchronization.
     */
    public enum SyncType {
        /**
         * Full sync - client should replace entire inventory with these entries.
         */
        FULL,

        /**
         * Incremental sync - client should update/add these entries.
         * Items with count 0 should be removed.
         */
        INCREMENTAL
    }

    /**
     * A single entry in the sync packet.
     * Represents an ItemStack and its count in the infinite inventory.
     */
    public record SyncEntry(ItemStack stack, long count) {
        /**
         * StreamCodec for SyncEntry serialization.
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStack.STREAM_CODEC, SyncEntry::stack,
                        ByteBufCodecs.VAR_LONG, SyncEntry::count,
                        SyncEntry::new
                );
    }

    // Custom packet payload type
    public static final Type<SyncInventoryPacket> TYPE =
            new Type<>(BottomlessNetworking.SYNC_INVENTORY_ID);

    /**
     * StreamCodec for SyncType enum serialization.
     * Uses a custom implementation to properly handle buffer types.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, SyncType> SYNC_TYPE_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncType decode(RegistryFriendlyByteBuf buf) {
                    int ordinal = buf.readVarInt();
                    return SyncType.values()[ordinal];
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SyncType value) {
                    buf.writeVarInt(value.ordinal());
                }
            };

    /**
     * StreamCodec for the entire packet.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInventoryPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncInventoryPacket::version,
                    SYNC_TYPE_CODEC, SyncInventoryPacket::syncType,
                    SyncEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncInventoryPacket::entries,
                    SyncInventoryPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // === Factory methods for creating packets ===

    /**
     * Creates a full sync packet from an InfiniteInventory.
     * The client should replace its entire inventory with the contents of this packet.
     *
     * @param inventory The inventory to sync
     * @return A new SyncInventoryPacket with FULL sync type
     */
    public static SyncInventoryPacket fullSync(InfiniteInventory inventory) {
        List<SyncEntry> entries = new ArrayList<>();

        if (inventory != null) {
            for (InfiniteInventoryEntry entry : inventory.getAllEntries()) {
                entries.add(new SyncEntry(entry.getReferenceStack(), entry.getCount()));
            }
        }

        return new SyncInventoryPacket(PACKET_VERSION, SyncType.FULL, entries);
    }

    /**
     * Creates an incremental sync packet for updated items.
     * The client should update/add these entries to its existing inventory.
     * Entries with count 0 indicate the item should be removed.
     *
     * @param updatedEntries List of entries that have changed
     * @return A new SyncInventoryPacket with INCREMENTAL sync type
     */
    public static SyncInventoryPacket incrementalSync(List<SyncEntry> updatedEntries) {
        return new SyncInventoryPacket(PACKET_VERSION, SyncType.INCREMENTAL, new ArrayList<>(updatedEntries));
    }

    /**
     * Creates an incremental sync packet for a single updated item.
     *
     * @param stack The item that changed
     * @param newCount The new count (0 means remove)
     * @return A new SyncInventoryPacket with INCREMENTAL sync type
     */
    public static SyncInventoryPacket incrementalSync(ItemStack stack, long newCount) {
        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(stack.copy(), newCount));
        return new SyncInventoryPacket(PACKET_VERSION, SyncType.INCREMENTAL, entries);
    }

    /**
     * Creates an empty full sync packet (clears the client inventory).
     *
     * @return A new SyncInventoryPacket with FULL sync type and no entries
     */
    public static SyncInventoryPacket emptySync() {
        return new SyncInventoryPacket(PACKET_VERSION, SyncType.FULL, new ArrayList<>());
    }

    // === Utility methods ===

    /**
     * Checks if this packet was created with a compatible version.
     *
     * @return true if the packet version is compatible with the current implementation
     */
    public boolean isCompatibleVersion() {
        // Currently only support exact version match
        // In the future, can add migration logic for older versions
        return version == PACKET_VERSION;
    }

    /**
     * Gets the number of entries in this packet.
     *
     * @return The number of entries
     */
    public int getEntryCount() {
        return entries.size();
    }

    /**
     * Checks if this is a full sync packet.
     *
     * @return true if this is a FULL sync
     */
    public boolean isFullSync() {
        return syncType == SyncType.FULL;
    }

    /**
     * Checks if this is an incremental sync packet.
     *
     * @return true if this is an INCREMENTAL sync
     */
    public boolean isIncrementalSync() {
        return syncType == SyncType.INCREMENTAL;
    }

    @Override
    public String toString() {
        return "SyncInventoryPacket{" +
                "version=" + version +
                ", syncType=" + syncType +
                ", entryCount=" + entries.size() +
                '}';
    }
}
