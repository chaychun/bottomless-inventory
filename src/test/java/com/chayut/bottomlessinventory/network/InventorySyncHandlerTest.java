package com.chayut.bottomlessinventory.network;

import com.chayut.bottomlessinventory.inventory.InfiniteInventory;
import com.chayut.bottomlessinventory.network.packets.InventoryActionPacket;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InventorySyncHandler.
 * Note: Full integration testing requires a running server environment.
 * These tests focus on the testable unit-level functionality:
 * - Rate limiting logic
 * - Packet validation
 * - Sync packet creation
 */
class InventorySyncHandlerTest {

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // === Rate Limiting Tests ===

    @Test
    void getMinActionIntervalMs_returnsConfiguredValue() {
        long interval = InventorySyncHandler.getMinActionIntervalMs();

        // Should be 50ms (20 actions per second)
        assertEquals(50, interval);
    }

    @Test
    void clearRateLimitTracking_removesPlayerFromTracking() {
        // This test verifies that the cleanup method exists and runs without error
        UUID playerId = UUID.randomUUID();

        // Clear tracking for a player that may or may not exist
        assertDoesNotThrow(() -> InventorySyncHandler.clearRateLimitTracking(playerId));
    }

    @Test
    void clearRateLimitTracking_handlesNonExistentPlayer() {
        // Clearing a player that was never tracked should not throw
        UUID nonExistentPlayer = UUID.randomUUID();

        assertDoesNotThrow(() -> InventorySyncHandler.clearRateLimitTracking(nonExistentPlayer));
    }

    // === Packet Validation Tests ===

    @Test
    void inventoryActionPacket_isValid_validPacket_returnsTrue() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        InventoryActionPacket packet = InventoryActionPacket.takeItems(stack, 64);

        assertTrue(packet.isValid());
    }

    @Test
    void inventoryActionPacket_isValid_invalidVersion_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION + 1,
                InventoryActionPacket.ActionType.TAKE_ITEMS,
                new ItemStack(Items.DIAMOND, 1),
                64
        );

        assertFalse(packet.isValid());
    }

    @Test
    void inventoryActionPacket_isValid_emptyStack_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION,
                InventoryActionPacket.ActionType.TAKE_ITEMS,
                ItemStack.EMPTY,
                64
        );

        assertFalse(packet.isValid());
    }

    @Test
    void inventoryActionPacket_isValid_zeroAmount_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION,
                InventoryActionPacket.ActionType.TAKE_ITEMS,
                new ItemStack(Items.DIAMOND, 1),
                0
        );

        assertFalse(packet.isValid());
    }

    @Test
    void inventoryActionPacket_isValid_negativeAmount_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION,
                InventoryActionPacket.ActionType.TAKE_ITEMS,
                new ItemStack(Items.DIAMOND, 1),
                -10
        );

        assertFalse(packet.isValid());
    }

    // === Sync Packet Creation Tests ===

    @Test
    void syncInventoryPacket_fullSync_emptyInventory_createsValidPacket() {
        InfiniteInventory inventory = new InfiniteInventory();

        SyncInventoryPacket packet = SyncInventoryPacket.fullSync(inventory);

        assertTrue(packet.isFullSync());
        assertTrue(packet.isCompatibleVersion());
        assertEquals(0, packet.getEntryCount());
    }

    @Test
    void syncInventoryPacket_fullSync_withItems_containsAllEntries() {
        InfiniteInventory inventory = new InfiniteInventory();
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 100);
        inventory.addItem(new ItemStack(Items.GOLD_INGOT, 1), 50);

        SyncInventoryPacket packet = SyncInventoryPacket.fullSync(inventory);

        assertTrue(packet.isFullSync());
        assertEquals(2, packet.getEntryCount());
    }

    @Test
    void syncInventoryPacket_fullSync_nullInventory_createsEmptyPacket() {
        SyncInventoryPacket packet = SyncInventoryPacket.fullSync(null);

        assertTrue(packet.isFullSync());
        assertEquals(0, packet.getEntryCount());
    }

    @Test
    void syncInventoryPacket_incrementalSync_singleItem_createsValidPacket() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        SyncInventoryPacket packet = SyncInventoryPacket.incrementalSync(stack, 50);

        assertTrue(packet.isIncrementalSync());
        assertTrue(packet.isCompatibleVersion());
        assertEquals(1, packet.getEntryCount());
    }

    @Test
    void syncInventoryPacket_incrementalSync_itemRemoval_hasZeroCount() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        // Create a removal packet (count = 0)
        SyncInventoryPacket packet = SyncInventoryPacket.incrementalSync(stack, 0);

        assertTrue(packet.isIncrementalSync());
        assertEquals(1, packet.getEntryCount());
        assertEquals(0, packet.entries().get(0).count());
    }

    @Test
    void syncInventoryPacket_incrementalSync_largeCount_handlesCorrectly() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        long largeCount = 9_999_999_999L;

        SyncInventoryPacket packet = SyncInventoryPacket.incrementalSync(stack, largeCount);

        assertEquals(largeCount, packet.entries().get(0).count());
    }

    @Test
    void syncInventoryPacket_emptySync_createsFullSyncWithNoEntries() {
        SyncInventoryPacket packet = SyncInventoryPacket.emptySync();

        assertTrue(packet.isFullSync());
        assertEquals(0, packet.getEntryCount());
    }

    // === Action Type Tests ===

    @Test
    void inventoryActionPacket_allActionTypes_areValid() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        for (InventoryActionPacket.ActionType actionType : InventoryActionPacket.ActionType.values()) {
            InventoryActionPacket packet = new InventoryActionPacket(
                    InventoryActionPacket.PACKET_VERSION,
                    actionType,
                    stack,
                    64
            );

            assertTrue(packet.isValid(),
                    "Packet with action type " + actionType + " should be valid");
        }
    }

    @Test
    void inventoryActionPacket_takeItems_isTakeAction() {
        InventoryActionPacket packet = InventoryActionPacket.takeItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );

        assertTrue(packet.isTakeAction());
        assertFalse(packet.isDepositAction());
        assertFalse(packet.isQuickMoveAction());
    }

    @Test
    void inventoryActionPacket_depositItems_isDepositAction() {
        InventoryActionPacket packet = InventoryActionPacket.depositItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );

        assertFalse(packet.isTakeAction());
        assertTrue(packet.isDepositAction());
        assertFalse(packet.isQuickMoveAction());
    }

    @Test
    void inventoryActionPacket_quickMove_isQuickMoveAction() {
        InventoryActionPacket packet = InventoryActionPacket.quickMove(
                new ItemStack(Items.DIAMOND, 1), 64
        );

        assertFalse(packet.isTakeAction());
        assertFalse(packet.isDepositAction());
        assertTrue(packet.isQuickMoveAction());
    }

    // === Inventory Integration Tests ===

    @Test
    void infiniteInventory_addAndRemove_maintainsCorrectCount() {
        InfiniteInventory inventory = new InfiniteInventory();
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        inventory.addItem(stack, 100);
        assertEquals(100, inventory.getCount(stack));

        long removed = inventory.removeItem(stack, 30);
        assertEquals(30, removed);
        assertEquals(70, inventory.getCount(stack));
    }

    @Test
    void infiniteInventory_removeMoreThanAvailable_returnsOnlyAvailable() {
        InfiniteInventory inventory = new InfiniteInventory();
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        inventory.addItem(stack, 50);

        long removed = inventory.removeItem(stack, 100);

        assertEquals(50, removed);
        assertEquals(0, inventory.getCount(stack));
    }

    @Test
    void infiniteInventory_afterRemoveAll_isEmpty() {
        InfiniteInventory inventory = new InfiniteInventory();
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        inventory.addItem(stack, 100);
        inventory.removeItem(stack, 100);

        assertFalse(inventory.contains(stack));
        assertTrue(inventory.isEmpty());
    }

    // === Sync Packet Entry Tests ===

    @Test
    void syncEntry_preservesStackAndCount() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD, 1);
        long count = 42;

        SyncInventoryPacket.SyncEntry entry = new SyncInventoryPacket.SyncEntry(stack, count);

        assertEquals(Items.DIAMOND_SWORD, entry.stack().getItem());
        assertEquals(count, entry.count());
    }
}
