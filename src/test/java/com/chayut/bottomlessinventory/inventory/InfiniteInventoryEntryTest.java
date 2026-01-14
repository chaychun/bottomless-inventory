package com.chayut.bottomlessinventory.inventory;

import net.fabricmc.loader.impl.util.log.LogLevel;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InfiniteInventoryEntry class.
 */
class InfiniteInventoryEntryTest {

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private ItemStack testStack;

    @BeforeEach
    void setUp() {
        testStack = new ItemStack(Items.DIAMOND, 1);
    }

    @Test
    void constructor_withValidInputs_createsEntry() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        assertEquals(100, entry.getCount());
        assertFalse(entry.isEmpty());
    }

    @Test
    void constructor_withStackCount64_normalizesTo1() {
        ItemStack stack64 = new ItemStack(Items.DIAMOND, 64);
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(stack64, 500);

        // Reference stack should be normalized to count of 1
        assertEquals(1, entry.getReferenceStack().getCount());
        assertEquals(500, entry.getCount());
    }

    @Test
    void constructor_withNullStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InfiniteInventoryEntry(null, 100);
        });
    }

    @Test
    void constructor_withEmptyStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InfiniteInventoryEntry(ItemStack.EMPTY, 100);
        });
    }

    @Test
    void constructor_withNegativeCount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InfiniteInventoryEntry(testStack, -1);
        });
    }

    @Test
    void constructor_withZeroCount_createsEmptyEntry() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 0);

        assertEquals(0, entry.getCount());
        assertTrue(entry.isEmpty());
    }

    @Test
    void setCount_withValidValue_updatesCount() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        entry.setCount(500);

        assertEquals(500, entry.getCount());
    }

    @Test
    void setCount_withNegativeValue_throwsException() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        assertThrows(IllegalArgumentException.class, () -> {
            entry.setCount(-1);
        });
    }

    @Test
    void addCount_withValidAmount_increasesCount() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        entry.addCount(50);

        assertEquals(150, entry.getCount());
    }

    @Test
    void addCount_withLargeValues_handlesCorrectly() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 1_000_000_000L);

        entry.addCount(1_000_000_000L);

        assertEquals(2_000_000_000L, entry.getCount());
    }

    @Test
    void addCount_withNegativeAmount_throwsException() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        assertThrows(IllegalArgumentException.class, () -> {
            entry.addCount(-1);
        });
    }

    @Test
    void removeCount_withAvailableAmount_returnsRequestedAmount() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        long removed = entry.removeCount(30);

        assertEquals(30, removed);
        assertEquals(70, entry.getCount());
    }

    @Test
    void removeCount_withMoreThanAvailable_returnsOnlyAvailable() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 50);

        long removed = entry.removeCount(100);

        assertEquals(50, removed);
        assertEquals(0, entry.getCount());
        assertTrue(entry.isEmpty());
    }

    @Test
    void removeCount_withExactAmount_makesEmpty() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        long removed = entry.removeCount(100);

        assertEquals(100, removed);
        assertTrue(entry.isEmpty());
    }

    @Test
    void removeCount_withNegativeAmount_throwsException() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        assertThrows(IllegalArgumentException.class, () -> {
            entry.removeCount(-1);
        });
    }

    @Test
    void getStackWithCount_returnsStackWithRequestedCount() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 1000);

        ItemStack result = entry.getStackWithCount(32);

        assertEquals(Items.DIAMOND, result.getItem());
        assertEquals(32, result.getCount());
    }

    @Test
    void getStackWithCount_clampsToMaxStackSize() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 1000);

        ItemStack result = entry.getStackWithCount(128);

        // Diamonds have max stack size of 64
        assertEquals(64, result.getCount());
    }

    @Test
    void getStackWithCount_withNegativeCount_throwsException() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        assertThrows(IllegalArgumentException.class, () -> {
            entry.getStackWithCount(-1);
        });
    }

    @Test
    void getReferenceStack_returnsCopyNotOriginal() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 100);

        ItemStack ref1 = entry.getReferenceStack();
        ItemStack ref2 = entry.getReferenceStack();

        // Should be equal but not the same instance
        assertTrue(ItemStack.isSameItemSameComponents(ref1, ref2));
        assertNotSame(ref1, ref2);
    }

    @Test
    void isEmpty_withZeroCount_returnsTrue() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 0);

        assertTrue(entry.isEmpty());
    }

    @Test
    void isEmpty_withPositiveCount_returnsFalse() {
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(testStack, 1);

        assertFalse(entry.isEmpty());
    }
}
