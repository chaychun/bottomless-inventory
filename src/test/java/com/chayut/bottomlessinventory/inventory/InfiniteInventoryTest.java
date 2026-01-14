package com.chayut.bottomlessinventory.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InfiniteInventory class.
 */
class InfiniteInventoryTest {

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private InfiniteInventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new InfiniteInventory();
    }

    // === Constructor and Basic State Tests ===

    @Test
    void newInventory_isEmpty() {
        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.getUniqueItemCount());
        assertEquals(0, inventory.getTotalItemCount());
    }

    // === addItem Tests ===

    @Test
    void addItem_withValidStack_addsToInventory() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        boolean result = inventory.addItem(diamonds, 100);

        assertTrue(result);
        assertEquals(100, inventory.getCount(diamonds));
        assertFalse(inventory.isEmpty());
    }

    @Test
    void addItem_multipleTimes_accumulatesCount() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        inventory.addItem(diamonds, 50);
        inventory.addItem(diamonds, 30);
        inventory.addItem(diamonds, 20);

        assertEquals(100, inventory.getCount(diamonds));
        assertEquals(1, inventory.getUniqueItemCount());
    }

    @Test
    void addItem_differentItems_createsSeparateEntries() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 1);

        inventory.addItem(diamonds, 50);
        inventory.addItem(gold, 30);

        assertEquals(50, inventory.getCount(diamonds));
        assertEquals(30, inventory.getCount(gold));
        assertEquals(2, inventory.getUniqueItemCount());
        assertEquals(80, inventory.getTotalItemCount());
    }

    @Test
    void addItem_withNullStack_returnsFalse() {
        boolean result = inventory.addItem(null, 100);

        assertFalse(result);
        assertTrue(inventory.isEmpty());
    }

    @Test
    void addItem_withEmptyStack_returnsFalse() {
        boolean result = inventory.addItem(ItemStack.EMPTY, 100);

        assertFalse(result);
        assertTrue(inventory.isEmpty());
    }

    @Test
    void addItem_withZeroCount_returnsFalse() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        boolean result = inventory.addItem(diamonds, 0);

        assertFalse(result);
        assertTrue(inventory.isEmpty());
    }

    @Test
    void addItem_withNegativeCount_returnsFalse() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        boolean result = inventory.addItem(diamonds, -10);

        assertFalse(result);
        assertTrue(inventory.isEmpty());
    }

    @Test
    void addItem_withLargeCount_handlesCorrectly() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        inventory.addItem(diamonds, 1_000_000_000L);
        inventory.addItem(diamonds, 1_000_000_000L);

        assertEquals(2_000_000_000L, inventory.getCount(diamonds));
    }

    // === removeItem Tests ===

    @Test
    void removeItem_withAvailableAmount_removesAndReturnsAmount() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 100);

        long removed = inventory.removeItem(diamonds, 30);

        assertEquals(30, removed);
        assertEquals(70, inventory.getCount(diamonds));
    }

    @Test
    void removeItem_withMoreThanAvailable_removesOnlyAvailable() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 50);

        long removed = inventory.removeItem(diamonds, 100);

        assertEquals(50, removed);
        assertEquals(0, inventory.getCount(diamonds));
        assertTrue(inventory.isEmpty());
    }

    @Test
    void removeItem_entireAmount_removesEntry() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 100);

        inventory.removeItem(diamonds, 100);

        assertFalse(inventory.contains(diamonds));
        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.getUniqueItemCount());
    }

    @Test
    void removeItem_nonExistentItem_returnsZero() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        long removed = inventory.removeItem(diamonds, 50);

        assertEquals(0, removed);
    }

    @Test
    void removeItem_withNullStack_returnsZero() {
        long removed = inventory.removeItem(null, 50);

        assertEquals(0, removed);
    }

    @Test
    void removeItem_withEmptyStack_returnsZero() {
        long removed = inventory.removeItem(ItemStack.EMPTY, 50);

        assertEquals(0, removed);
    }

    @Test
    void removeItem_withZeroCount_returnsZero() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 100);

        long removed = inventory.removeItem(diamonds, 0);

        assertEquals(0, removed);
        assertEquals(100, inventory.getCount(diamonds));
    }

    @Test
    void removeItem_withNegativeCount_returnsZero() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 100);

        long removed = inventory.removeItem(diamonds, -10);

        assertEquals(0, removed);
        assertEquals(100, inventory.getCount(diamonds));
    }

    // === getCount Tests ===

    @Test
    void getCount_existingItem_returnsCorrectCount() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 42);

        assertEquals(42, inventory.getCount(diamonds));
    }

    @Test
    void getCount_nonExistentItem_returnsZero() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        assertEquals(0, inventory.getCount(diamonds));
    }

    @Test
    void getCount_withNullStack_returnsZero() {
        assertEquals(0, inventory.getCount(null));
    }

    @Test
    void getCount_withEmptyStack_returnsZero() {
        assertEquals(0, inventory.getCount(ItemStack.EMPTY));
    }

    // === contains Tests ===

    @Test
    void contains_existingItem_returnsTrue() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 1);

        assertTrue(inventory.contains(diamonds));
    }

    @Test
    void contains_nonExistentItem_returnsFalse() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        assertFalse(inventory.contains(diamonds));
    }

    @Test
    void contains_afterRemovingAllItems_returnsFalse() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 10);
        inventory.removeItem(diamonds, 10);

        assertFalse(inventory.contains(diamonds));
    }

    // === getAllEntries Tests ===

    @Test
    void getAllEntries_emptyInventory_returnsEmptyCollection() {
        Collection<InfiniteInventoryEntry> entries = inventory.getAllEntries();

        assertTrue(entries.isEmpty());
    }

    @Test
    void getAllEntries_withItems_returnsAllEntries() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 50);
        inventory.addItem(new ItemStack(Items.GOLD_INGOT, 1), 30);
        inventory.addItem(new ItemStack(Items.IRON_INGOT, 1), 20);

        Collection<InfiniteInventoryEntry> entries = inventory.getAllEntries();

        assertEquals(3, entries.size());
    }

    // === clear Tests ===

    @Test
    void clear_removesAllItems() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 50);
        inventory.addItem(new ItemStack(Items.GOLD_INGOT, 1), 30);

        inventory.clear();

        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.getUniqueItemCount());
        assertEquals(0, inventory.getTotalItemCount());
    }

    @Test
    void clear_onEmptyInventory_doesNothing() {
        inventory.clear();

        assertTrue(inventory.isEmpty());
    }

    // === getUniqueItemCount Tests ===

    @Test
    void getUniqueItemCount_countsDistinctItemTypes() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 100);
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 50); // Same type
        inventory.addItem(new ItemStack(Items.GOLD_INGOT, 1), 30);

        assertEquals(2, inventory.getUniqueItemCount());
    }

    // === getTotalItemCount Tests ===

    @Test
    void getTotalItemCount_sumsAllCounts() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 100);
        inventory.addItem(new ItemStack(Items.GOLD_INGOT, 1), 50);
        inventory.addItem(new ItemStack(Items.IRON_INGOT, 1), 25);

        assertEquals(175, inventory.getTotalItemCount());
    }

    // === Item Stack Count Independence ===

    @Test
    void addItem_stackCountDoesNotAffectStoredCount() {
        // Stack with count 64
        ItemStack stack64 = new ItemStack(Items.DIAMOND, 64);
        // Stack with count 1
        ItemStack stack1 = new ItemStack(Items.DIAMOND, 1);

        inventory.addItem(stack64, 100);

        // Should be able to query with stack of different count
        assertEquals(100, inventory.getCount(stack1));
        assertEquals(100, inventory.getCount(stack64));
    }
}
