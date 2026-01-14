package com.chayut.bottomlessinventory.network.packets;

import com.chayut.bottomlessinventory.network.BottomlessNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Client -> Server packet for requesting item operations on the infinite inventory.
 * The server will validate these requests and perform the actions if allowed.
 */
public record InventoryActionPacket(
        int version,
        ActionType actionType,
        ItemStack targetStack,
        long amount
) implements CustomPacketPayload {

    /**
     * Current packet version for forward/backward compatibility.
     * Increment when packet format changes.
     */
    public static final int PACKET_VERSION = 1;

    /**
     * Types of inventory actions that can be requested.
     */
    public enum ActionType {
        /**
         * Take items from the infinite inventory into player inventory.
         * The targetStack specifies which item type, amount specifies how many.
         */
        TAKE_ITEMS,

        /**
         * Deposit items from player inventory into the infinite inventory.
         * The targetStack specifies which item type, amount specifies how many.
         */
        DEPOSIT_ITEMS,

        /**
         * Quick-move operation - typically shift-click behavior.
         * Moves items between player inventory and infinite inventory automatically.
         */
        QUICK_MOVE
    }

    // Custom packet payload type
    public static final Type<InventoryActionPacket> TYPE =
            new Type<>(BottomlessNetworking.INVENTORY_ACTION_ID);

    /**
     * StreamCodec for ActionType enum serialization.
     * Uses a custom implementation to properly handle buffer types.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, ActionType> ACTION_TYPE_CODEC =
            new StreamCodec<>() {
                @Override
                public ActionType decode(RegistryFriendlyByteBuf buf) {
                    int ordinal = buf.readVarInt();
                    return ActionType.values()[ordinal];
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, ActionType value) {
                    buf.writeVarInt(value.ordinal());
                }
            };

    /**
     * StreamCodec for the entire packet.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryActionPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InventoryActionPacket::version,
                    ACTION_TYPE_CODEC, InventoryActionPacket::actionType,
                    ItemStack.STREAM_CODEC, InventoryActionPacket::targetStack,
                    ByteBufCodecs.VAR_LONG, InventoryActionPacket::amount,
                    InventoryActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // === Factory methods for creating packets ===

    /**
     * Creates a take items request.
     * Requests to take items from the infinite inventory into the player's inventory.
     *
     * @param stack The item type to take
     * @param amount The number of items to take
     * @return A new InventoryActionPacket with TAKE_ITEMS action
     */
    public static InventoryActionPacket takeItems(ItemStack stack, long amount) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Target stack cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return new InventoryActionPacket(PACKET_VERSION, ActionType.TAKE_ITEMS, stack.copy(), amount);
    }

    /**
     * Creates a deposit items request.
     * Requests to deposit items from the player's inventory into the infinite inventory.
     *
     * @param stack The item type to deposit
     * @param amount The number of items to deposit
     * @return A new InventoryActionPacket with DEPOSIT_ITEMS action
     */
    public static InventoryActionPacket depositItems(ItemStack stack, long amount) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Target stack cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return new InventoryActionPacket(PACKET_VERSION, ActionType.DEPOSIT_ITEMS, stack.copy(), amount);
    }

    /**
     * Creates a quick-move request.
     * Typically used for shift-click behavior to quickly move items.
     *
     * @param stack The item type to quick-move
     * @param amount The number of items to move
     * @return A new InventoryActionPacket with QUICK_MOVE action
     */
    public static InventoryActionPacket quickMove(ItemStack stack, long amount) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Target stack cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return new InventoryActionPacket(PACKET_VERSION, ActionType.QUICK_MOVE, stack.copy(), amount);
    }

    // === Utility methods ===

    /**
     * Checks if this packet was created with a compatible version.
     *
     * @return true if the packet version is compatible with the current implementation
     */
    public boolean isCompatibleVersion() {
        // Currently only support exact version match
        // In the future, can add migration logic for older versions
        return version == PACKET_VERSION;
    }

    /**
     * Validates that the packet contains valid data.
     * Does not check server-side game state (like whether player has items).
     *
     * @return true if the packet data is valid
     */
    public boolean isValid() {
        if (!isCompatibleVersion()) {
            return false;
        }
        if (targetStack == null || targetStack.isEmpty()) {
            return false;
        }
        if (amount <= 0) {
            return false;
        }
        if (actionType == null) {
            return false;
        }
        return true;
    }

    /**
     * Checks if this is a take items action.
     *
     * @return true if actionType is TAKE_ITEMS
     */
    public boolean isTakeAction() {
        return actionType == ActionType.TAKE_ITEMS;
    }

    /**
     * Checks if this is a deposit items action.
     *
     * @return true if actionType is DEPOSIT_ITEMS
     */
    public boolean isDepositAction() {
        return actionType == ActionType.DEPOSIT_ITEMS;
    }

    /**
     * Checks if this is a quick-move action.
     *
     * @return true if actionType is QUICK_MOVE
     */
    public boolean isQuickMoveAction() {
        return actionType == ActionType.QUICK_MOVE;
    }

    @Override
    public String toString() {
        return "InventoryActionPacket{" +
                "version=" + version +
                ", actionType=" + actionType +
                ", targetStack=" + targetStack.getItem() +
                ", amount=" + amount +
                '}';
    }
}
