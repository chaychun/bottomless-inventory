package com.chayut.bottomlessinventory.client;

import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket.SyncEntry;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket.SyncType;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClientInventoryCache functionality.
 * Note: These tests do not test packet networking (which requires full client setup),
 * but do test the cache logic itself.
 */
class ClientInventoryCacheTest {

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void resetCache() {
        // Reset the singleton before each test
        ClientInventoryCache.resetInstance();
    }

    // === Singleton Tests ===

    @Test
    void getInstance_returnsSameInstance() {
        ClientInventoryCache instance1 = ClientInventoryCache.getInstance();
        ClientInventoryCache instance2 = ClientInventoryCache.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void resetInstance_createsNewInstance() {
        ClientInventoryCache instance1 = ClientInventoryCache.getInstance();
        ClientInventoryCache.resetInstance();
        ClientInventoryCache instance2 = ClientInventoryCache.getInstance();

        assertNotSame(instance1, instance2);
    }

    // === Full Sync Tests ===

    @Test
    void handleSync_fullSync_replacesEntireCache() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Add initial items via full sync
        List<SyncEntry> initialEntries = new ArrayList<>();
        initialEntries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));
        initialEntries.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 200L));

        SyncInventoryPacket initialPacket = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                initialEntries
        );

        cache.handleSync(initialPacket);

        assertEquals(2, cache.getTotalUniqueItems());
        assertEquals(100L, cache.getCount(new ItemStack(Items.DIAMOND)));
        assertEquals(200L, cache.getCount(new ItemStack(Items.GOLD_INGOT)));

        // Replace with new full sync
        List<SyncEntry> newEntries = new ArrayList<>();
        newEntries.add(new SyncEntry(new ItemStack(Items.EMERALD, 1), 300L));

        SyncInventoryPacket newPacket = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                newEntries
        );

        cache.handleSync(newPacket);

        // Old items should be gone
        assertEquals(1, cache.getTotalUniqueItems());
        assertEquals(0L, cache.getCount(new ItemStack(Items.DIAMOND)));
        assertEquals(0L, cache.getCount(new ItemStack(Items.GOLD_INGOT)));
        assertEquals(300L, cache.getCount(new ItemStack(Items.EMERALD)));
    }

    @Test
    void handleSync_fullSync_empty_clearsCache() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Add initial items
        List<SyncEntry> initialEntries = new ArrayList<>();
        initialEntries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        SyncInventoryPacket initialPacket = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                initialEntries
        );

        cache.handleSync(initialPacket);
        assertFalse(cache.isEmpty());

        // Send empty full sync
        SyncInventoryPacket emptyPacket = SyncInventoryPacket.emptySync();
        cache.handleSync(emptyPacket);

        assertTrue(cache.isEmpty());
    }

    @Test
    void handleSync_fullSync_skipsEmptyStacks() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));
        entries.add(new SyncEntry(ItemStack.EMPTY, 50L)); // Should be skipped

        SyncInventoryPacket packet = new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        );

        cache.handleSync(packet);

        assertEquals(1, cache.getTotalUniqueItems());
    }

    // === Incremental Sync Tests ===

    @Test
    void handleSync_incrementalSync_addsNewItem() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Start with one item
        List<SyncEntry> initialEntries = new ArrayList<>();
        initialEntries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                initialEntries
        ));

        // Incrementally add another item
        cache.handleSync(SyncInventoryPacket.incrementalSync(new ItemStack(Items.GOLD_INGOT, 1), 200L));

        assertEquals(2, cache.getTotalUniqueItems());
        assertEquals(100L, cache.getCount(new ItemStack(Items.DIAMOND)));
        assertEquals(200L, cache.getCount(new ItemStack(Items.GOLD_INGOT)));
    }

    @Test
    void handleSync_incrementalSync_updatesExistingItem() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Start with one item
        List<SyncEntry> initialEntries = new ArrayList<>();
        initialEntries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                initialEntries
        ));

        // Update the count
        cache.handleSync(SyncInventoryPacket.incrementalSync(new ItemStack(Items.DIAMOND, 1), 250L));

        assertEquals(1, cache.getTotalUniqueItems());
        assertEquals(250L, cache.getCount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void handleSync_incrementalSync_removesItemWithZeroCount() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Start with two items
        List<SyncEntry> initialEntries = new ArrayList<>();
        initialEntries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));
        initialEntries.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 200L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                initialEntries
        ));

        // Remove diamond by sending count 0
        cache.handleSync(SyncInventoryPacket.incrementalSync(new ItemStack(Items.DIAMOND, 1), 0L));

        assertEquals(1, cache.getTotalUniqueItems());
        assertEquals(0L, cache.getCount(new ItemStack(Items.DIAMOND)));
        assertEquals(200L, cache.getCount(new ItemStack(Items.GOLD_INGOT)));
    }

    @Test
    void handleSync_incrementalSync_multipleEntries() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Start with two items
        List<SyncEntry> initialEntries = new ArrayList<>();
        initialEntries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));
        initialEntries.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 200L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                initialEntries
        ));

        // Send incremental update with multiple changes
        List<SyncEntry> updates = new ArrayList<>();
        updates.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 150L));     // Update
        updates.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 0L));    // Remove
        updates.add(new SyncEntry(new ItemStack(Items.EMERALD, 1), 300L));     // Add new

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.INCREMENTAL,
                updates
        ));

        assertEquals(2, cache.getTotalUniqueItems());
        assertEquals(150L, cache.getCount(new ItemStack(Items.DIAMOND)));
        assertEquals(0L, cache.getCount(new ItemStack(Items.GOLD_INGOT)));
        assertEquals(300L, cache.getCount(new ItemStack(Items.EMERALD)));
    }

    // === Query Method Tests ===

    @Test
    void getCount_emptyCache_returnsZero() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        assertEquals(0L, cache.getCount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void getCount_nullStack_returnsZero() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        assertEquals(0L, cache.getCount(null));
    }

    @Test
    void getCount_emptyStack_returnsZero() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        assertEquals(0L, cache.getCount(ItemStack.EMPTY));
    }

    @Test
    void getAllEntries_returnsDefensiveCopy() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        // Get entries and modify the collection
        var allEntries = cache.getAllEntries();
        int originalSize = allEntries.size();
        allEntries.clear();

        // Original cache should be unaffected
        assertEquals(originalSize, cache.getTotalUniqueItems());
    }

    @Test
    void contains_existingItem_returnsTrue() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        assertTrue(cache.contains(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void contains_nonExistingItem_returnsFalse() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        assertFalse(cache.contains(new ItemStack(Items.GOLD_INGOT)));
    }

    @Test
    void isEmpty_emptyCache_returnsTrue() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        assertTrue(cache.isEmpty());
    }

    @Test
    void isEmpty_nonEmptyCache_returnsFalse() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        assertFalse(cache.isEmpty());
    }

    @Test
    void getTotalUniqueItems_countsCorrectly() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));
        entries.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 200L));
        entries.add(new SyncEntry(new ItemStack(Items.EMERALD, 1), 300L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        assertEquals(3, cache.getTotalUniqueItems());
    }

    // === Sorted Entries Tests ===

    @Test
    void getSortedEntries_sortsAlphabetically() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Add items in non-alphabetical order
        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 200L));  // minecraft:gold_ingot
        entries.add(new SyncEntry(new ItemStack(Items.APPLE, 1), 50L));         // minecraft:apple
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));      // minecraft:diamond

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        var sortedEntries = cache.getSortedEntries();

        assertEquals(3, sortedEntries.size());
        // Should be sorted: apple, diamond, gold_ingot
        assertEquals(Items.APPLE, sortedEntries.get(0).getReferenceStack().getItem());
        assertEquals(Items.DIAMOND, sortedEntries.get(1).getReferenceStack().getItem());
        assertEquals(Items.GOLD_INGOT, sortedEntries.get(2).getReferenceStack().getItem());
    }

    @Test
    void getSortedEntries_emptyCache_returnsEmptyList() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        var sortedEntries = cache.getSortedEntries();

        assertNotNull(sortedEntries);
        assertTrue(sortedEntries.isEmpty());
    }

    @Test
    void getSortedEntries_returnsDefensiveCopy() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        // Get sorted entries and modify
        var sortedEntries = cache.getSortedEntries();
        sortedEntries.clear();

        // Original cache should be unaffected
        assertEquals(1, cache.getTotalUniqueItems());
    }

    // === Item with Components Tests ===

    @Test
    void handleSync_itemWithComponents_trackedSeparately() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // Create regular diamond
        ItemStack regularDiamond = new ItemStack(Items.DIAMOND, 1);

        // Create named diamond
        ItemStack namedDiamond = new ItemStack(Items.DIAMOND, 1);
        namedDiamond.set(DataComponents.CUSTOM_NAME, Component.literal("Magic Diamond"));

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(regularDiamond, 100L));
        entries.add(new SyncEntry(namedDiamond, 50L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        // Should be tracked as two separate items
        assertEquals(2, cache.getTotalUniqueItems());
        assertEquals(100L, cache.getCount(regularDiamond));
        assertEquals(50L, cache.getCount(namedDiamond));
    }

    // === Version Compatibility Tests ===

    @Test
    void handleSync_incompatibleVersion_ignoresPacket() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        // First, add some items with valid packet
        List<SyncEntry> initialEntries = new ArrayList<>();
        initialEntries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                initialEntries
        ));

        assertEquals(1, cache.getTotalUniqueItems());

        // Send packet with incompatible version
        List<SyncEntry> newEntries = new ArrayList<>();
        newEntries.add(new SyncEntry(new ItemStack(Items.EMERALD, 1), 200L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION + 999, // Wrong version
                SyncType.FULL,
                newEntries
        ));

        // Cache should be unchanged
        assertEquals(1, cache.getTotalUniqueItems());
        assertEquals(100L, cache.getCount(new ItemStack(Items.DIAMOND)));
    }

    // === Clear Tests ===

    @Test
    void clear_emptiesCache() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        assertFalse(cache.isEmpty());

        cache.clear();

        assertTrue(cache.isEmpty());
    }

    // === CachedEntry Tests ===

    @Test
    void cachedEntry_getReferenceStack_returnsDefensiveCopy() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        var allEntries = cache.getAllEntries();
        var entry = allEntries.iterator().next();

        // Modify returned stack
        ItemStack stack = entry.getReferenceStack();
        stack.setCount(64);

        // Original should be unaffected (always count 1)
        assertEquals(1, entry.getReferenceStack().getCount());
    }

    @Test
    void cachedEntry_toString_containsRelevantInfo() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 100L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        var entry = cache.getAllEntries().iterator().next();
        String str = entry.toString();

        assertTrue(str.contains("diamond"));
        assertTrue(str.contains("100"));
    }

    // === Edge Cases ===

    @Test
    void handleSync_largeCount_handledCorrectly() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        long largeCount = Long.MAX_VALUE - 1;

        List<SyncEntry> entries = new ArrayList<>();
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), largeCount));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        assertEquals(largeCount, cache.getCount(new ItemStack(Items.DIAMOND)));
    }

    @Test
    void handleSync_manyItems_handledCorrectly() {
        ClientInventoryCache cache = ClientInventoryCache.getInstance();

        List<SyncEntry> entries = new ArrayList<>();
        // Add many different item types
        entries.add(new SyncEntry(new ItemStack(Items.DIAMOND, 1), 1L));
        entries.add(new SyncEntry(new ItemStack(Items.GOLD_INGOT, 1), 2L));
        entries.add(new SyncEntry(new ItemStack(Items.IRON_INGOT, 1), 3L));
        entries.add(new SyncEntry(new ItemStack(Items.EMERALD, 1), 4L));
        entries.add(new SyncEntry(new ItemStack(Items.COAL, 1), 5L));
        entries.add(new SyncEntry(new ItemStack(Items.REDSTONE, 1), 6L));
        entries.add(new SyncEntry(new ItemStack(Items.LAPIS_LAZULI, 1), 7L));
        entries.add(new SyncEntry(new ItemStack(Items.COPPER_INGOT, 1), 8L));
        entries.add(new SyncEntry(new ItemStack(Items.NETHERITE_INGOT, 1), 9L));
        entries.add(new SyncEntry(new ItemStack(Items.AMETHYST_SHARD, 1), 10L));

        cache.handleSync(new SyncInventoryPacket(
                SyncInventoryPacket.PACKET_VERSION,
                SyncType.FULL,
                entries
        ));

        assertEquals(10, cache.getTotalUniqueItems());
    }
}
