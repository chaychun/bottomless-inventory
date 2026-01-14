package com.chayut.bottomlessinventory.inventory;

import net.minecraft.world.item.ItemStack;

/**
 * Represents a single entry in the infinite inventory.
 * Contains a reference ItemStack (the template) and a count representing how many of that item are stored.
 */
public class InfiniteInventoryEntry {
    private final ItemStack referenceStack;
    private long count;

    /**
     * Creates a new inventory entry.
     * @param referenceStack The template ItemStack (must have count of 1)
     * @param count The number of items stored
     */
    public InfiniteInventoryEntry(ItemStack referenceStack, long count) {
        if (referenceStack == null || referenceStack.isEmpty()) {
            throw new IllegalArgumentException("Reference ItemStack cannot be null or empty");
        }
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }

        // Store a copy with count of 1 as the reference
        this.referenceStack = referenceStack.copy();
        this.referenceStack.setCount(1);
        this.count = count;
    }

    /**
     * Gets the reference ItemStack (template).
     * This is the single item that represents the type and components.
     * @return A copy of the reference ItemStack with count 1
     */
    public ItemStack getReferenceStack() {
        return referenceStack.copy();
    }

    /**
     * Gets the total count of items stored.
     * @return The count
     */
    public long getCount() {
        return count;
    }

    /**
     * Sets the count of items stored.
     * @param count The new count (must be non-negative)
     */
    public void setCount(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        this.count = count;
    }

    /**
     * Adds to the current count.
     * @param amount The amount to add (must be non-negative)
     */
    public void addCount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to add cannot be negative");
        }
        this.count += amount;
    }

    /**
     * Removes from the current count.
     * @param amount The amount to remove (must be non-negative and not exceed current count)
     * @return The actual amount removed
     */
    public long removeCount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to remove cannot be negative");
        }

        long actualRemoved = Math.min(amount, this.count);
        this.count -= actualRemoved;
        return actualRemoved;
    }

    /**
     * Gets a copy of the ItemStack with a specific count.
     * Useful for converting back to regular ItemStacks for vanilla inventory operations.
     * @param count The count to set (clamped to ItemStack max stack size)
     * @return A new ItemStack with the specified count
     */
    public ItemStack getStackWithCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }

        ItemStack result = referenceStack.copy();
        // Clamp to max stack size
        int actualCount = Math.min(count, result.getMaxStackSize());
        result.setCount(actualCount);
        return result;
    }

    /**
     * Checks if this entry is empty (count is 0).
     * @return true if count is 0
     */
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public String toString() {
        return "InfiniteInventoryEntry{" +
                "item=" + referenceStack.getItem() +
                ", count=" + count +
                '}';
    }
}
