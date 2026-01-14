package com.chayut.bottomlessinventory.inventory;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ItemStackKey class.
 * Verifies proper equality and hashing behavior for use in HashMaps.
 */
class ItemStackKeyTest {

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // === Constructor Tests ===

    @Test
    void constructor_withValidStack_createsKey() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        ItemStackKey key = new ItemStackKey(stack);

        assertNotNull(key);
        assertEquals(Items.DIAMOND, key.getItem());
    }

    @Test
    void constructor_withNullStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ItemStackKey(null);
        });
    }

    @Test
    void constructor_withEmptyStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ItemStackKey(ItemStack.EMPTY);
        });
    }

    // === Equality Tests ===

    @Test
    void equals_sameItemType_returnsTrue() {
        ItemStack stack1 = new ItemStack(Items.DIAMOND, 1);
        ItemStack stack2 = new ItemStack(Items.DIAMOND, 1);

        ItemStackKey key1 = new ItemStackKey(stack1);
        ItemStackKey key2 = new ItemStackKey(stack2);

        assertEquals(key1, key2);
    }

    @Test
    void equals_differentItemTypes_returnsFalse() {
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 1);

        ItemStackKey key1 = new ItemStackKey(diamonds);
        ItemStackKey key2 = new ItemStackKey(gold);

        assertNotEquals(key1, key2);
    }

    @Test
    void equals_sameItemDifferentCount_returnsTrue() {
        // Stack count should NOT affect key equality
        ItemStack stack1 = new ItemStack(Items.DIAMOND, 1);
        ItemStack stack64 = new ItemStack(Items.DIAMOND, 64);

        ItemStackKey key1 = new ItemStackKey(stack1);
        ItemStackKey key64 = new ItemStackKey(stack64);

        assertEquals(key1, key64);
    }

    @Test
    void equals_sameItemDifferentCustomName_returnsFalse() {
        ItemStack stack1 = new ItemStack(Items.DIAMOND, 1);
        stack1.set(DataComponents.CUSTOM_NAME, Component.literal("Special Diamond"));

        ItemStack stack2 = new ItemStack(Items.DIAMOND, 1);
        stack2.set(DataComponents.CUSTOM_NAME, Component.literal("Other Diamond"));

        ItemStackKey key1 = new ItemStackKey(stack1);
        ItemStackKey key2 = new ItemStackKey(stack2);

        assertNotEquals(key1, key2);
    }

    @Test
    void equals_namedVsUnnamed_returnsFalse() {
        ItemStack named = new ItemStack(Items.DIAMOND, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named Diamond"));

        ItemStack unnamed = new ItemStack(Items.DIAMOND, 1);

        ItemStackKey keyNamed = new ItemStackKey(named);
        ItemStackKey keyUnnamed = new ItemStackKey(unnamed);

        assertNotEquals(keyNamed, keyUnnamed);
    }

    @Test
    void equals_sameObject_returnsTrue() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        ItemStackKey key = new ItemStackKey(stack);

        assertEquals(key, key);
    }

    @Test
    void equals_withNull_returnsFalse() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        ItemStackKey key = new ItemStackKey(stack);

        assertNotEquals(key, null);
    }

    @Test
    void equals_withDifferentType_returnsFalse() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        ItemStackKey key = new ItemStackKey(stack);

        assertNotEquals(key, "not a key");
    }

    // === HashCode Tests ===

    @Test
    void hashCode_sameItems_returnsSameCode() {
        ItemStack stack1 = new ItemStack(Items.DIAMOND, 1);
        ItemStack stack2 = new ItemStack(Items.DIAMOND, 1);

        ItemStackKey key1 = new ItemStackKey(stack1);
        ItemStackKey key2 = new ItemStackKey(stack2);

        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void hashCode_isConsistent() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        ItemStackKey key = new ItemStackKey(stack);

        int hash1 = key.hashCode();
        int hash2 = key.hashCode();
        int hash3 = key.hashCode();

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    // === HashMap Integration Tests ===

    @Test
    void hashMapUsage_canStoreAndRetrieve() {
        Map<ItemStackKey, Integer> map = new HashMap<>();
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);

        ItemStackKey key = new ItemStackKey(diamonds);
        map.put(key, 100);

        assertEquals(100, map.get(key));
    }

    @Test
    void hashMapUsage_differentStacksSameTypeSameKey() {
        Map<ItemStackKey, Integer> map = new HashMap<>();
        ItemStack stack1 = new ItemStack(Items.DIAMOND, 1);
        ItemStack stack2 = new ItemStack(Items.DIAMOND, 64);

        map.put(new ItemStackKey(stack1), 100);

        // Should find the value with a different ItemStack instance of the same type
        assertEquals(100, map.get(new ItemStackKey(stack2)));
    }

    @Test
    void hashMapUsage_differentTypesAreSeparate() {
        Map<ItemStackKey, Integer> map = new HashMap<>();
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 1);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 1);

        map.put(new ItemStackKey(diamonds), 100);
        map.put(new ItemStackKey(gold), 50);

        assertEquals(100, map.get(new ItemStackKey(diamonds)));
        assertEquals(50, map.get(new ItemStackKey(gold)));
        assertEquals(2, map.size());
    }

    @Test
    void hashMapUsage_namedItemsSeparateFromUnnamed() {
        Map<ItemStackKey, Integer> map = new HashMap<>();

        ItemStack named = new ItemStack(Items.DIAMOND, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        ItemStack unnamed = new ItemStack(Items.DIAMOND, 1);

        map.put(new ItemStackKey(named), 100);
        map.put(new ItemStackKey(unnamed), 50);

        assertEquals(100, map.get(new ItemStackKey(named)));
        assertEquals(50, map.get(new ItemStackKey(unnamed)));
        assertEquals(2, map.size());
    }

    // === Getters Tests ===

    @Test
    void getItem_returnsCorrectItem() {
        ItemStack stack = new ItemStack(Items.GOLD_INGOT, 1);
        ItemStackKey key = new ItemStackKey(stack);

        assertEquals(Items.GOLD_INGOT, key.getItem());
    }

    @Test
    void getComponents_returnsComponents() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Test"));

        ItemStackKey key = new ItemStackKey(stack);

        assertNotNull(key.getComponents());
    }
}
