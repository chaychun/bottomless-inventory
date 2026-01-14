package com.chayut.bottomlessinventory.network;

import com.chayut.bottomlessinventory.BottomlessInventory;
import com.chayut.bottomlessinventory.network.packets.InventoryActionPacket;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Central networking setup class for the Bottomless Inventory mod.
 * Registers all packet types and provides utilities for sending packets.
 */
public class BottomlessNetworking {

    // Packet identifiers
    public static final ResourceLocation SYNC_INVENTORY_ID =
            ResourceLocation.fromNamespaceAndPath(BottomlessInventory.MOD_ID, "sync_inventory");
    public static final ResourceLocation INVENTORY_ACTION_ID =
            ResourceLocation.fromNamespaceAndPath(BottomlessInventory.MOD_ID, "inventory_action");

    /**
     * Registers all packet types for the mod.
     * Should be called from BottomlessInventory.onInitialize().
     */
    public static void register() {
        BottomlessInventory.LOGGER.info("Registering Bottomless Inventory networking");

        // Register Server -> Client packets (play phase)
        PayloadTypeRegistry.playS2C().register(
                SyncInventoryPacket.TYPE,
                SyncInventoryPacket.CODEC
        );

        // Register Client -> Server packets (play phase)
        PayloadTypeRegistry.playC2S().register(
                InventoryActionPacket.TYPE,
                InventoryActionPacket.CODEC
        );

        BottomlessInventory.LOGGER.info("Bottomless Inventory networking registered successfully");
    }
}
