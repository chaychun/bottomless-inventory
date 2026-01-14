package com.chayut.bottomlessinventory.network;

import com.chayut.bottomlessinventory.BottomlessInventory;
import com.chayut.bottomlessinventory.network.packets.OpenInventoryPacket;
import com.chayut.bottomlessinventory.screen.BottomlessScreenHandlerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles the OpenInventoryPacket sent from clients.
 * Opens the bottomless inventory screen for the requesting player.
 */
public class OpenInventoryHandler {

    /**
     * Registers the packet receiver for opening the bottomless inventory.
     * Should be called from BottomlessInventory.onInitialize() AFTER BottomlessNetworking.register().
     */
    public static void register() {
        BottomlessInventory.LOGGER.info("Registering OpenInventoryHandler");

        ServerPlayNetworking.registerGlobalReceiver(OpenInventoryPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();

            // Execute on server thread to safely open the menu
            context.server().execute(() -> {
                player.openMenu(new BottomlessScreenHandlerFactory());
                BottomlessInventory.LOGGER.debug("Opened bottomless inventory for player: {}",
                    player.getName().getString());
            });
        });

        BottomlessInventory.LOGGER.info("OpenInventoryHandler registered successfully");
    }
}
