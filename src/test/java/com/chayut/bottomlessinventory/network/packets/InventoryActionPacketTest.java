package com.chayut.bottomlessinventory.network.packets;

import com.chayut.bottomlessinventory.network.packets.InventoryActionPacket.ActionType;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InventoryActionPacket serialization and deserialization.
 */
class InventoryActionPacketTest {

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

    // === Basic Round-trip Tests ===

    @Test
    void inventoryActionPacket_takeItems_roundTrip() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        InventoryActionPacket original = InventoryActionPacket.takeItems(stack, 64);

        RegistryFriendlyByteBuf buffer = createBuffer();
        InventoryActionPacket.CODEC.encode(buffer, original);

        InventoryActionPacket decoded = InventoryActionPacket.CODEC.decode(buffer);

        assertEquals(InventoryActionPacket.PACKET_VERSION, decoded.version());
        assertEquals(ActionType.TAKE_ITEMS, decoded.actionType());
        assertEquals(Items.DIAMOND, decoded.targetStack().getItem());
        assertEquals(64, decoded.amount());
        assertTrue(decoded.isTakeAction());
    }

    @Test
    void inventoryActionPacket_depositItems_roundTrip() {
        ItemStack stack = new ItemStack(Items.GOLD_INGOT, 1);
        InventoryActionPacket original = InventoryActionPacket.depositItems(stack, 100);

        RegistryFriendlyByteBuf buffer = createBuffer();
        InventoryActionPacket.CODEC.encode(buffer, original);

        InventoryActionPacket decoded = InventoryActionPacket.CODEC.decode(buffer);

        assertEquals(ActionType.DEPOSIT_ITEMS, decoded.actionType());
        assertEquals(Items.GOLD_INGOT, decoded.targetStack().getItem());
        assertEquals(100, decoded.amount());
        assertTrue(decoded.isDepositAction());
    }

    @Test
    void inventoryActionPacket_quickMove_roundTrip() {
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 1);
        InventoryActionPacket original = InventoryActionPacket.quickMove(stack, 32);

        RegistryFriendlyByteBuf buffer = createBuffer();
        InventoryActionPacket.CODEC.encode(buffer, original);

        InventoryActionPacket decoded = InventoryActionPacket.CODEC.decode(buffer);

        assertEquals(ActionType.QUICK_MOVE, decoded.actionType());
        assertEquals(Items.IRON_INGOT, decoded.targetStack().getItem());
        assertEquals(32, decoded.amount());
        assertTrue(decoded.isQuickMoveAction());
    }

    // === Item with Components Tests ===

    @Test
    void inventoryActionPacket_withNamedItem_preservesComponents() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Excalibur"));
        InventoryActionPacket original = InventoryActionPacket.takeItems(stack, 1);

        RegistryFriendlyByteBuf buffer = createBuffer();
        InventoryActionPacket.CODEC.encode(buffer, original);

        InventoryActionPacket decoded = InventoryActionPacket.CODEC.decode(buffer);

        assertTrue(decoded.targetStack().has(DataComponents.CUSTOM_NAME));
        assertEquals("Excalibur", decoded.targetStack().get(DataComponents.CUSTOM_NAME).getString());
    }

    // === Large Amount Tests ===

    @Test
    void inventoryActionPacket_largeAmount_roundTrip() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        long largeAmount = 9_999_999_999L;
        InventoryActionPacket original = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION,
                ActionType.TAKE_ITEMS,
                stack,
                largeAmount
        );

        RegistryFriendlyByteBuf buffer = createBuffer();
        InventoryActionPacket.CODEC.encode(buffer, original);

        InventoryActionPacket decoded = InventoryActionPacket.CODEC.decode(buffer);

        assertEquals(largeAmount, decoded.amount());
    }

    // === Factory Method Tests ===

    @Test
    void takeItems_createsCorrectPacket() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        InventoryActionPacket packet = InventoryActionPacket.takeItems(stack, 50);

        assertEquals(InventoryActionPacket.PACKET_VERSION, packet.version());
        assertEquals(ActionType.TAKE_ITEMS, packet.actionType());
        assertEquals(50, packet.amount());
        assertTrue(packet.isValid());
    }

    @Test
    void depositItems_createsCorrectPacket() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        InventoryActionPacket packet = InventoryActionPacket.depositItems(stack, 75);

        assertEquals(ActionType.DEPOSIT_ITEMS, packet.actionType());
        assertEquals(75, packet.amount());
        assertTrue(packet.isValid());
    }

    @Test
    void quickMove_createsCorrectPacket() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);

        InventoryActionPacket packet = InventoryActionPacket.quickMove(stack, 64);

        assertEquals(ActionType.QUICK_MOVE, packet.actionType());
        assertEquals(64, packet.amount());
        assertTrue(packet.isValid());
    }

    // === Factory Method Validation Tests ===

    @Test
    void takeItems_withNullStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            InventoryActionPacket.takeItems(null, 64);
        });
    }

    @Test
    void takeItems_withEmptyStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            InventoryActionPacket.takeItems(ItemStack.EMPTY, 64);
        });
    }

    @Test
    void takeItems_withZeroAmount_throwsException() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        assertThrows(IllegalArgumentException.class, () -> {
            InventoryActionPacket.takeItems(stack, 0);
        });
    }

    @Test
    void takeItems_withNegativeAmount_throwsException() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 1);
        assertThrows(IllegalArgumentException.class, () -> {
            InventoryActionPacket.takeItems(stack, -10);
        });
    }

    @Test
    void depositItems_withNullStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            InventoryActionPacket.depositItems(null, 64);
        });
    }

    @Test
    void quickMove_withNullStack_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            InventoryActionPacket.quickMove(null, 64);
        });
    }

    // === Validation Tests ===

    @Test
    void isValid_validPacket_returnsTrue() {
        InventoryActionPacket packet = InventoryActionPacket.takeItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );

        assertTrue(packet.isValid());
    }

    @Test
    void isValid_incompatibleVersion_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION + 1,
                ActionType.TAKE_ITEMS,
                new ItemStack(Items.DIAMOND, 1),
                64
        );

        assertFalse(packet.isValid());
    }

    @Test
    void isValid_emptyStack_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION,
                ActionType.TAKE_ITEMS,
                ItemStack.EMPTY,
                64
        );

        assertFalse(packet.isValid());
    }

    @Test
    void isValid_zeroAmount_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION,
                ActionType.TAKE_ITEMS,
                new ItemStack(Items.DIAMOND, 1),
                0
        );

        assertFalse(packet.isValid());
    }

    @Test
    void isValid_negativeAmount_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION,
                ActionType.TAKE_ITEMS,
                new ItemStack(Items.DIAMOND, 1),
                -10
        );

        assertFalse(packet.isValid());
    }

    // === Version Compatibility Tests ===

    @Test
    void isCompatibleVersion_currentVersion_returnsTrue() {
        InventoryActionPacket packet = InventoryActionPacket.takeItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );

        assertTrue(packet.isCompatibleVersion());
    }

    @Test
    void isCompatibleVersion_differentVersion_returnsFalse() {
        InventoryActionPacket packet = new InventoryActionPacket(
                InventoryActionPacket.PACKET_VERSION + 1,
                ActionType.TAKE_ITEMS,
                new ItemStack(Items.DIAMOND, 1),
                64
        );

        assertFalse(packet.isCompatibleVersion());
    }

    // === Action Type Helper Tests ===

    @Test
    void actionTypeHelpers_returnCorrectValues() {
        InventoryActionPacket takePacket = InventoryActionPacket.takeItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );
        assertTrue(takePacket.isTakeAction());
        assertFalse(takePacket.isDepositAction());
        assertFalse(takePacket.isQuickMoveAction());

        InventoryActionPacket depositPacket = InventoryActionPacket.depositItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );
        assertFalse(depositPacket.isTakeAction());
        assertTrue(depositPacket.isDepositAction());
        assertFalse(depositPacket.isQuickMoveAction());

        InventoryActionPacket quickMovePacket = InventoryActionPacket.quickMove(
                new ItemStack(Items.DIAMOND, 1), 64
        );
        assertFalse(quickMovePacket.isTakeAction());
        assertFalse(quickMovePacket.isDepositAction());
        assertTrue(quickMovePacket.isQuickMoveAction());
    }

    // === All Action Types Round-trip ===

    @Test
    void allActionTypes_roundTrip() {
        for (ActionType actionType : ActionType.values()) {
            InventoryActionPacket original = new InventoryActionPacket(
                    InventoryActionPacket.PACKET_VERSION,
                    actionType,
                    new ItemStack(Items.DIAMOND, 1),
                    42
            );

            RegistryFriendlyByteBuf buffer = createBuffer();
            InventoryActionPacket.CODEC.encode(buffer, original);

            InventoryActionPacket decoded = InventoryActionPacket.CODEC.decode(buffer);

            assertEquals(actionType, decoded.actionType(),
                    "ActionType " + actionType + " should survive round-trip");
        }
    }

    // === Type Tests ===

    @Test
    void type_returnsCorrectType() {
        InventoryActionPacket packet = InventoryActionPacket.takeItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );

        assertEquals(InventoryActionPacket.TYPE, packet.type());
    }

    // === ToString Tests ===

    @Test
    void toString_containsRelevantInfo() {
        InventoryActionPacket packet = InventoryActionPacket.takeItems(
                new ItemStack(Items.DIAMOND, 1), 64
        );
        String str = packet.toString();

        assertTrue(str.contains("version="));
        assertTrue(str.contains("actionType=TAKE_ITEMS"));
        assertTrue(str.contains("amount=64"));
    }

    // === Stack Copy Test ===

    @Test
    void factoryMethods_createStackCopy() {
        ItemStack original = new ItemStack(Items.DIAMOND, 1);
        InventoryActionPacket packet = InventoryActionPacket.takeItems(original, 64);

        // Modify the original stack
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Modified"));

        // Packet should not be affected
        assertFalse(packet.targetStack().has(DataComponents.CUSTOM_NAME));
    }
}
