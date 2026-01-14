package com.chayut.bottomlessinventory.inventory;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * A key class for ItemStack comparison in HashMaps.
 * Two ItemStacks are considered equal if they have the same:
 * - Item type (Registry ID)
 * - Data components (NBT equivalent in 1.21+)
 */
public class ItemStackKey {
    private final Item item;
    private final DataComponentPatch components;
    private final int cachedHashCode;

    /**
     * Creates a key from an ItemStack.
     * @param stack The ItemStack to create a key from
     */
    public ItemStackKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot create ItemStackKey from null or empty ItemStack");
        }

        this.item = stack.getItem();
        // Get the component patch which contains all the data components
        this.components = stack.getComponentsPatch();

        // Pre-calculate hash code since this object is immutable
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        return Objects.hash(
            BuiltInRegistries.ITEM.getKey(item),
            components
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ItemStackKey other = (ItemStackKey) obj;

        // Compare item types by registry ID
        if (!BuiltInRegistries.ITEM.getKey(this.item).equals(BuiltInRegistries.ITEM.getKey(other.item))) {
            return false;
        }

        // Compare components
        return Objects.equals(this.components, other.components);
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    /**
     * Gets the item type this key represents.
     * @return The Item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Gets the data components this key represents.
     * @return The DataComponentPatch
     */
    public DataComponentPatch getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return "ItemStackKey{" +
                "item=" + BuiltInRegistries.ITEM.getKey(item) +
                ", components=" + components +
                '}';
    }
}
