package com.chayut.bottomlessinventory.network.packets;

import com.chayut.bottomlessinventory.inventory.InfiniteInventory;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket.SyncEntry;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket.SyncType;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SyncInventoryPacket serialization and deserialization.
 */
class SyncInventoryPacketTest {

    private static RegistryAccess.Frozen registryAccess;

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private RegistryFriendlyByteBuf createBuffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
    }

    // === SyncEntry Tests ===

    @Test
    void syncEntry_roundTrip_preservesData() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        SyncEntry original = new SyncEntry(stack, 12345L);

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncEntry.STREAM_CODEC.encode(buffer, original);

        SyncEntry decoded = SyncEntry.STREAM_CODEC.decode(buffer);

        assertEquals(Items.DIAMOND, decoded.stack().getItem());
        assertEquals(12345L, decoded.count());
    }

    @Test
    void syncEntry_withNamedItem_preservesComponents() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Magic Diamond"));
        SyncEntry original = new SyncEntry(stack, 100L);

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncEntry.STREAM_CODEC.encode(buffer, original);

        SyncEntry decoded = SyncEntry.STREAM_CODEC.decode(buffer);

        assertEquals(Items.DIAMOND, decoded.stack().getItem());
        assertEquals(100L, decoded.count());
        assertTrue(decoded.stack().has(DataComponents.CUSTOM_NAME));
        assertEquals("Magic Diamond", decoded.stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void syncEntry_withLargeCount_preservesValue() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        long largeCount = 9_999_999_999L;
        SyncEntry original = new SyncEntry(stack, largeCount);

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncEntry.STREAM_CODEC.encode(buffer, original);

        SyncEntry decoded = SyncEntry.STREAM_CODEC.decode(buffer);

        assertEquals(largeCount, decoded.count());
    }

    @Test
    void syncEntry_withZeroCount_preservesValue() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        SyncEntry original = new SyncEntry(stack, 0L);

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncEntry.STREAM_CODEC.encode(buffer, original);

        SyncEntry decoded = SyncEntry.STREAM_CODEC.decode(buffer);

        assertEquals(0L, decoded.count());
    }

    // === Full Packet Tests ===

    @Test
    void syncInventoryPacket_fullSync_roundTrip() {
        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));
        entries.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 200L));

        SyncInventoryPacket original = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        );

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncInventoryPacket.CODEC.encode(buffer, original);

        SyncInventoryPacket decoded = SyncInventoryPacket.CODEC.decode(buffer);

        assertEquals(SyncInventoryPacket.PACKET_VERSION, decoded.version());
        assertEquals(SyncType.FULL, decoded.syncType());
        assertEquals(2, decoded.entries().size());
        assertTrue(decoded.isFullSync());
        assertFalse(decoded.isIncrementalSync());
    }

    @Test
    void syncInventoryPacket_incrementalSync_roundTrip() {
        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.IRON_INGOT, 1), 50L));

        SyncInventoryPacket original = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.INCREMENTAL,
                entries
        );

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncInventoryPacket.CODEC.encode(buffer, original);

        SyncInventoryPacket decoded = SyncInventoryPacket.CODEC.decode(buffer);

        assertEquals(SyncType.INCREMENTAL, decoded.syncType());
        assertEquals(1, decoded.entries().size());
        assertTrue(decoded.isIncrementalSync());
        assertFalse(decoded.isFullSync());
    }

    @Test
    void syncInventoryPacket_emptyList_roundTrip() {
        SyncInventoryPacket original = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                new ArrayList<>()
        );

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncInventoryPacket.CODEC.encode(buffer, original);

        SyncInventoryPacket decoded = SyncInventoryPacket.CODEC.decode(buffer);

        assertEquals(0, decoded.getEntryCount());
        assertTrue(decoded.entries().isEmpty());
    }

    // === Factory Method Tests ===

    @Test
    void fullSync_fromInventory_createsCorrectPacket() {
        InfiniteInventory inventory = new InfiniteInventory();
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 100);
        inventory.addItem(new ItemStack(Items.EMERALD, 1), 50);

        SyncInventoryPacket packet = SyncInventoryPacket.fullSync(inventory);

        assertEquals(SyncInventoryPacket.PACKET_VERSION, packet.version());
        assertEquals(SyncType.FULL, packet.syncType());
        assertEquals(2, packet.getEntryCount());
        assertTrue(packet.isCompatibleVersion());
    }

    @Test
    void fullSync_fromEmptyInventory_createsEmptyPacket() {
        InfiniteInventory inventory = new InfiniteInventory();

        SyncInventoryPacket packet = SyncInventoryPacket.fullSync(inventory);

        assertEquals(SyncType.FULL, packet.syncType());
        assertEquals(0, packet.getEntryCount());
    }

    @Test
    void fullSync_fromNullInventory_createsEmptyPacket() {
        SyncInventoryPacket packet = SyncInventoryPacket.fullSync(null);

        assertEquals(SyncType.FULL, packet.syncType());
        assertEquals(0, packet.getEntryCount());
    }

    @Test
    void incrementalSync_withList_createsCorrectPacket() {
        List<SyncEntry> updates = new ArrayList<>();
        updates.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 150L));
        updates.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 0L)); // Remove signal

        SyncInventoryPacket packet = SyncInventoryPacket.incrementalSync(updates);

        assertEquals(SyncType.INCREMENTAL, packet.syncType());
        assertEquals(2, packet.getEntryCount());
    }

    @Test
    void incrementalSync_singleItem_createsCorrectPacket() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        SyncInventoryPacket packet = SyncInventoryPacket.incrementalSync(stack, 500L);

        assertEquals(SyncType.INCREMENTAL, packet.syncType());
        assertEquals(1, packet.getEntryCount());
        assertEquals(500L, packet.entries().get(0).count());
    }

    @Test
    void emptySync_createsCorrectPacket() {
        SyncInventoryPacket packet = SyncInventoryPacket.emptySync();

        assertEquals(SyncType.FULL, packet.syncType());
        assertEquals(0, packet.getEntryCount());
        assertTrue(packet.isFullSync());
    }

    // === Type Tests ===

    @Test
    void type_returnsCorrectType() {
        SyncInventoryPacket packet = SyncInventoryPacket.emptySync();

        assertEquals(SyncInventoryPacket.TYPE, packet.type());
    }

    // === Version Compatibility Tests ===

    @Test
    void isCompatibleVersion_currentVersion_returnsTrue() {
        SyncInventoryPacket packet = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                new ArrayList<>()
        );

        assertTrue(packet.isCompatibleVersion());
    }

    @Test
    void isCompatibleVersion_differentVersion_returnsFalse() {
        SyncInventoryPacket packet = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION + 1,
                SyncType.FULL,
                new ArrayList<>()
        );

        assertFalse(packet.isCompatibleVersion());
    }

    // === Large Data Tests ===

    @Test
    void syncInventoryPacket_manyEntries_roundTrip() {
        List<SyncEntry> entries = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), i * 100L));
        }

        SyncInventoryPacket original = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        );

        RegistryFriendlyByteBuf buffer = createBuffer();
        SyncInventoryPacket.CODEC.encode(buffer, original);

        SyncInventoryPacket decoded = SyncInventoryPacket.CODEC.decode(buffer);

        assertEquals(100, decoded.getEntryCount());
    }

    // === ToString Tests ===

    @Test
    void toString_containsRelevantInfo() {
        SyncInventoryPacket packet = SyncInventoryPacket.emptySync();
        String str = packet.toString();

        assertTrue(str.contains("version="));
        assertTrue(str.contains("syncType=FULL"));
        assertTrue(str.contains("entryCount=0"));
    }
}
