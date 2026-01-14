package com.chayut.bottomlessinventory.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating BottomlessScreenHandler instances.
 * Implements MenuProvider to integrate with Minecraft's screen opening system.
 */
public class BottomlessScreenHandlerFactory implements MenuProvider {

    /**
     * Gets the display name for the bottomless inventory screen.
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.bottomless_inventory");
    }

    /**
     * Creates a new BottomlessScreenHandler instance.
     *
     * @param syncId The synchronization ID for this screen handler
     * @param playerInventory The player's inventory
     * @param player The player opening the screen
     * @return A new BottomlessScreenHandler instance
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        // Use ContainerLevelAccess.NULL for now - will be set properly when opening the screen
        return new BottomlessScreenHandler(syncId, playerInventory);
    }
}
