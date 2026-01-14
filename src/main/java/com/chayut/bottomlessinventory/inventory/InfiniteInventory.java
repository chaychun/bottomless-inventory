package com.chayut.bottomlessinventory.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Main storage class for the infinite inventory system.
 * Uses a HashMap with ItemStackKey to store arbitrary amounts of items.
 */
public class InfiniteInventory {
    private final Map<ItemStackKey, InfiniteInventoryEntry> storage;

    /**
     * Creates a new empty infinite inventory.
     */
    public InfiniteInventory() {
        this.storage = new HashMap<>();
    }

    /**
     * Adds items to the inventory.
     * If the item type already exists, increases the count.
     * If the item type is new, creates a new entry.
     *
     * @param stack The ItemStack to add (used as template)
     * @param count The number of items to add
     * @return true if items were successfully added
     */
    public boolean addItem(ItemStack stack, long count) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (count <= 0) {
            return false;
        }

        ItemStackKey key = new ItemStackKey(stack);
        InfiniteInventoryEntry entry = storage.get(key);

        if (entry == null) {
            // Create new entry
            entry = new InfiniteInventoryEntry(stack, count);
            storage.put(key, entry);
        } else {
            // Add to existing entry
            entry.addCount(count);
        }

        return true;
    }

    /**
     * Removes items from the inventory.
     * Returns the actual number removed (may be less than requested if not enough items).
     * Automatically removes the entry if count reaches 0.
     *
     * @param stack The ItemStack to remove (used as key)
     * @param count The number of items to remove
     * @return The actual number of items removed
     */
    public long removeItem(ItemStack stack, long count) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (count <= 0) {
            return 0;
        }

        ItemStackKey key = new ItemStackKey(stack);
        InfiniteInventoryEntry entry = storage.get(key);

        if (entry == null) {
            return 0;
        }

        long removed = entry.removeCount(count);

        // Clean up empty entries
        if (entry.isEmpty()) {
            storage.remove(key);
        }

        return removed;
    }

    /**
     * Gets the count of a specific item type in the inventory.
     *
     * @param stack The ItemStack to check (used as key)
     * @return The count of matching items, or 0 if not present
     */
    public long getCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        ItemStackKey key = new ItemStackKey(stack);
        InfiniteInventoryEntry entry = storage.get(key);

        return entry != null ? entry.getCount() : 0;
    }

    /**
     * Gets all entries in the inventory.
     * Useful for rendering UI or iterating through all stored items.
     *
     * @return A collection of all inventory entries
     */
    public Collection<InfiniteInventoryEntry> getAllEntries() {
        return storage.values();
    }

    /**
     * Checks if the inventory is empty.
     *
     * @return true if no items are stored
     */
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    /**
     * Removes all items from the inventory.
     */
    public void clear() {
        storage.clear();
    }

    /**
     * Gets the number of different item types stored.
     *
     * @return The number of unique item types
     */
    public int getUniqueItemCount() {
        return storage.size();
    }

    /**
     * Gets the total count of all items across all types.
     * Note: May overflow if total exceeds Long.MAX_VALUE.
     *
     * @return The total count of all items
     */
    public long getTotalItemCount() {
        return storage.values().stream()
                .mapToLong(InfiniteInventoryEntry::getCount)
                .sum();
    }

    /**
     * Checks if the inventory contains a specific item type.
     *
     * @param stack The ItemStack to check
     * @return true if at least one of this item type is stored
     */
    public boolean contains(ItemStack stack) {
        return getCount(stack) > 0;
    }

    /**
     * Serializes this inventory to NBT.
     *
     * @param registryAccess Registry access for ItemStack serialization
     * @return CompoundTag containing all inventory data
     */
    public CompoundTag toNbt(HolderLookup.Provider registryAccess) {
        return InfiniteInventorySerializer.serialize(this, registryAccess);
    }

    /**
     * Deserializes an inventory from NBT.
     * Creates a new InfiniteInventory instance with the data from the tag.
     *
     * @param tag The NBT tag to read from
     * @param registryAccess Registry access for ItemStack deserialization
     * @return A new InfiniteInventory populated with the data from the tag
     */
    public static InfiniteInventory fromNbt(CompoundTag tag, HolderLookup.Provider registryAccess) {
        return InfiniteInventorySerializer.deserialize(tag, registryAccess);
    }

    @Override
    public String toString() {
        return "InfiniteInventory{" +
                "uniqueItems=" + getUniqueItemCount() +
                ", totalItems=" + getTotalItemCount() +
                '}';
    }
}
