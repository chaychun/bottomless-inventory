package com.chayut.bottomlessinventory.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InfiniteInventorySerializer class.
 * Tests NBT serialization and deserialization of inventory data.
 */
class InfiniteInventorySerializerTest {

    private static HolderLookup.Provider registryAccess;

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private InfiniteInventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new InfiniteInventory();
    }

    // === serialize Tests ===

    @Test
    void serialize_emptyInventory_createsValidTag() {
        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);

        assertNotNull(tag);
        assertEquals(1, tag.getInt("Version").orElse(0));
        assertTrue(tag.contains("Items"));
    }

    @Test
    void serialize_nullInventory_createsEmptyTag() {
        CompoundTag tag = InfiniteInventorySerializer.serialize(null, registryAccess);

        assertNotNull(tag);
        assertEquals(1, tag.getInt("Version").orElse(0));
    }

    @Test
    void serialize_withItems_includesAllItems() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 100);
        inventory.addItem(new ItemStack(Items.GOLD_INGOT, 1), 50);

        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);

        assertNotNull(tag);
        assertEquals(2, tag.getList("Items").orElseThrow().size());
    }

    // === deserialize Tests ===

    @Test
    void deserialize_nullTag_returnsEmptyInventory() {
        InfiniteInventory result = InfiniteInventorySerializer.deserialize(null, registryAccess);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deserialize_emptyTag_returnsEmptyInventory() {
        CompoundTag tag = new CompoundTag();

        InfiniteInventory result = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // === Round-trip Tests ===

    @Test
    void roundTrip_emptyInventory_preservesState() {
        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);
        InfiniteInventory restored = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertTrue(restored.isEmpty());
        assertEquals(0, restored.getUniqueItemCount());
    }

    @Test
    void roundTrip_singleItem_preservesData() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        inventory.addItem(diamonds, 12345);

        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);
        InfiniteInventory restored = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertEquals(1, restored.getUniqueItemCount());
        assertEquals(12345, restored.getCount(diamonds));
    }

    @Test
    void roundTrip_multipleItems_preservesAllData() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 100);
        inventory.addItem(new ItemStack(Items.GOLD_INGOT, 1), 200);
        inventory.addItem(new ItemStack(Items.IRON_INGOT, 1), 300);

        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);
        InfiniteInventory restored = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertEquals(3, restored.getUniqueItemCount());
        assertEquals(600, restored.getTotalItemCount());
        assertEquals(100, restored.getCount(new ItemStack(Items.DIAMOND, 1)));
        assertEquals(200, restored.getCount(new ItemStack(Items.GOLD_INGOT, 1)));
        assertEquals(300, restored.getCount(new ItemStack(Items.IRON_INGOT, 1)));
    }

    @Test
    void roundTrip_largeCount_preservesValue() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        long largeCount = 1_000_000_000L;
        inventory.addItem(diamonds, largeCount);

        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);
        InfiniteInventory restored = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertEquals(largeCount, restored.getCount(diamonds));
    }

    @Test
    void roundTrip_namedItem_preservesName() {
        ItemStack named = new ItemStack(Items.DIAMOND, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("My Special Diamond"));
        inventory.addItem(named, 42);

        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);
        InfiniteInventory restored = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertEquals(42, restored.getCount(named));

        // Verify unnamed diamond is NOT found
        ItemStack unnamed = new ItemStack(Items.DIAMOND, 1);
        assertEquals(0, restored.getCount(unnamed));
    }

    @Test
    void roundTrip_namedAndUnnamedSameItem_keepsSeparate() {
        ItemStack named = new ItemStack(Items.DIAMOND, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack unnamed = new ItemStack(Items.DIAMOND, 1);

        inventory.addItem(named, 100);
        inventory.addItem(unnamed, 50);

        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);
        InfiniteInventory restored = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertEquals(2, restored.getUniqueItemCount());
        assertEquals(100, restored.getCount(named));
        assertEquals(50, restored.getCount(unnamed));
    }

    // === Entry serialization Tests ===

    @Test
    void writeEntry_withValidEntry_createsTag() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(stack, 100);

        CompoundTag tag = InfiniteInventorySerializer.writeEntry(entry, registryAccess);

        assertNotNull(tag);
        assertTrue(tag.contains("Stack"));
        assertEquals(100L, tag.getLong("Count").orElse(0L));
    }

    @Test
    void writeEntry_withNullEntry_returnsNull() {
        CompoundTag tag = InfiniteInventorySerializer.writeEntry(null, registryAccess);

        assertNull(tag);
    }

    @Test
    void writeEntry_withEmptyEntry_returnsNull() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        InfiniteInventoryEntry entry = new InfiniteInventoryEntry(stack, 0);

        CompoundTag tag = InfiniteInventorySerializer.writeEntry(entry, registryAccess);

        assertNull(tag);
    }

    @Test
    void readEntry_withValidTag_createsEntry() {
        // First write an entry
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        InfiniteInventoryEntry original = new InfiniteInventoryEntry(stack, 100);
        CompoundTag tag = InfiniteInventorySerializer.writeEntry(original, registryAccess);

        // Then read it back
        InfiniteInventoryEntry restored = InfiniteInventorySerializer.readEntry(tag, registryAccess);

        assertNotNull(restored);
        assertEquals(100, restored.getCount());
        assertEquals(Items.DIAMOND, restored.getReferenceStack().getItem());
    }

    @Test
    void readEntry_withNullTag_returnsNull() {
        InfiniteInventoryEntry entry = InfiniteInventorySerializer.readEntry(null, registryAccess);

        assertNull(entry);
    }

    @Test
    void readEntry_withMissingStackKey_returnsNull() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Count", 100);
        // Missing "Stack" key

        InfiniteInventoryEntry entry = InfiniteInventorySerializer.readEntry(tag, registryAccess);

        assertNull(entry);
    }

    @Test
    void readEntry_withMissingCountKey_returnsNull() {
        // This is trickier - we need a valid stack tag but missing count
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        InfiniteInventoryEntry original = new InfiniteInventoryEntry(stack, 100);
        CompoundTag tag = InfiniteInventorySerializer.writeEntry(original, registryAccess);
        tag.remove("Count");

        InfiniteInventoryEntry entry = InfiniteInventorySerializer.readEntry(tag, registryAccess);

        assertNull(entry);
    }

    @Test
    void readEntry_withZeroCount_returnsNull() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        InfiniteInventoryEntry original = new InfiniteInventoryEntry(stack, 100);
        CompoundTag tag = InfiniteInventorySerializer.writeEntry(original, registryAccess);
        tag.putLong("Count", 0);

        InfiniteInventoryEntry entry = InfiniteInventorySerializer.readEntry(tag, registryAccess);

        assertNull(entry);
    }

    // === Version handling Tests ===

    @Test
    void deserialize_currentVersion_loadsSuccessfully() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 100);
        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);

        InfiniteInventory restored = InfiniteInventorySerializer.deserialize(tag, registryAccess);

        assertEquals(100, restored.getCount(new ItemStack(Items.DIAMOND, 1)));
    }

    @Test
    void serialize_includesVersionTag() {
        CompoundTag tag = InfiniteInventorySerializer.serialize(inventory, registryAccess);

        assertEquals(InfiniteInventorySerializer.VERSION, tag.getInt("Version").orElse(0));
    }

    // === toNbt/fromNbt convenience methods ===

    @Test
    void toNbtFromNbt_roundTrip_preservesData() {
        inventory.addItem(new ItemStack(Items.DIAMOND, 1), 500);
        inventory.addItem(new ItemStack(Items.EMERALD, 1), 250);

        CompoundTag tag = inventory.toNbt(registryAccess);
        InfiniteInventory restored = InfiniteInventory.fromNbt(tag, registryAccess);

        assertEquals(2, restored.getUniqueItemCount());
        assertEquals(750, restored.getTotalItemCount());
    }
}
