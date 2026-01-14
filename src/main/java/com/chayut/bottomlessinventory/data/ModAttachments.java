package com.chayut.bottomlessinventory.data;

import com.chayut.bottomlessinventory.BottomlessInventory;
import com.chayut.bottomlessinventory.inventory.InfiniteInventory;
import com.chayut.bottomlessinventory.inventory.InfiniteInventorySerializer;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Defines and registers all attachment types for the mod.
 * Uses Fabric's Attachment API to store persistent data on entities (specifically players).
 *
 * The inventory is stored as raw NBT (CompoundTag) because Fabric's Codec-based
 * serialization doesn't provide HolderLookup.Provider during save/load.
 * Use the helper methods getInventory() and setInventory() to work with InfiniteInventory.
 */
public class ModAttachments {
    /**
     * Attachment type for storing the infinite inventory data on players.
     * Stores raw NBT that gets converted to/from InfiniteInventory via helper methods.
     */
    public static final AttachmentType<CompoundTag> INFINITE_INVENTORY_DATA = AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .initializer(() -> InfiniteInventorySerializer.serialize(new InfiniteInventory(), null))
            .copyOnDeath()
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(
                    BottomlessInventory.MOD_ID,
                    "infinite_inventory"
            ));

    /**
     * Gets the InfiniteInventory for a player.
     * Deserializes from the stored NBT data.
     *
     * @param player The player to get inventory for
     * @param registryAccess Registry access from player's level
     * @return The player's infinite inventory
     */
    public static InfiniteInventory getInventory(Player player, HolderLookup.Provider registryAccess) {
        CompoundTag data = player.getAttachedOrCreate(INFINITE_INVENTORY_DATA);
        return InfiniteInventorySerializer.deserialize(data, registryAccess);
    }

    /**
     * Sets the InfiniteInventory for a player.
     * Serializes to NBT and stores it.
     *
     * @param player The player to set inventory for
     * @param inventory The inventory to store
     * @param registryAccess Registry access from player's level
     */
    public static void setInventory(Player player, InfiniteInventory inventory, HolderLookup.Provider registryAccess) {
        CompoundTag data = InfiniteInventorySerializer.serialize(inventory, registryAccess);
        player.setAttached(INFINITE_INVENTORY_DATA, data);
    }

    /**
     * Convenience method to get inventory using player's level for registry access.
     *
     * @param player The player (must be in a level/world)
     * @return The player's infinite inventory
     */
    public static InfiniteInventory getInventory(Player player) {
        return getInventory(player, player.registryAccess());
    }

    /**
     * Convenience method to set inventory using player's level for registry access.
     *
     * @param player The player (must be in a level/world)
     * @param inventory The inventory to store
     */
    public static void setInventory(Player player, InfiniteInventory inventory) {
        setInventory(player, inventory, player.registryAccess());
    }

    /**
     * Initializes all attachments.
     * Called during mod initialization to ensure attachments are registered.
     */
    public static void register() {
        BottomlessInventory.LOGGER.info("Registered attachment: {}", INFINITE_INVENTORY_DATA);
    }
}
