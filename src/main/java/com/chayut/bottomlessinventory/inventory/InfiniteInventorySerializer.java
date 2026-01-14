package com.chayut.bottomlessinventory.inventory;

import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Helper class for serializing and deserializing InfiniteInventory to/from NBT.
 * Handles versioning, edge cases, and error recovery.
 */
public class InfiniteInventorySerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfiniteInventorySerializer.class);

    /**
     * Current serialization version.
     * Increment this when changing the NBT format to support migration.
     */
    public static final int VERSION = 1;

    // NBT keys
    private static final String KEY_VERSION = "Version";
    private static final String KEY_ITEMS = "Items";
    private static final String KEY_STACK = "Stack";
    private static final String KEY_COUNT = "Count";

    /**
     * Serializes a single inventory entry to NBT.
     *
     * @param entry The entry to serialize
     * @param registryAccess Registry access for ItemStack serialization
     * @return CompoundTag containing the entry data, or null if serialization fails
     */
    public static CompoundTag writeEntry(InfiniteInventoryEntry entry, HolderLookup.Provider registryAccess) {
        if (entry == null || entry.isEmpty()) {
            return null;
        }

        try {
            CompoundTag entryTag = new CompoundTag();

            // Serialize the reference ItemStack using Codec
            ItemStack stack = entry.getReferenceStack();
            DataResult<Tag> encodeResult = ItemStack.CODEC.encodeStart(
                registryAccess.createSerializationContext(NbtOps.INSTANCE),
                stack
            );

            Tag stackTag = encodeResult.getOrThrow();
            entryTag.put(KEY_STACK, stackTag);

            // Store the count
            entryTag.putLong(KEY_COUNT, entry.getCount());

            return entryTag;
        } catch (Exception e) {
            LOGGER.error("Failed to serialize inventory entry: {}", entry, e);
            return null;
        }
    }

    /**
     * Deserializes a single inventory entry from NBT.
     *
     * @param tag The tag to read from
     * @param registryAccess Registry access for ItemStack deserialization
     * @return The deserialized entry, or null if deserialization fails or data is invalid
     */
    public static InfiniteInventoryEntry readEntry(CompoundTag tag, HolderLookup.Provider registryAccess) {
        if (tag == null) {
            return null;
        }

        try {
            // Check for required keys
            if (!tag.contains(KEY_STACK) || !tag.contains(KEY_COUNT)) {
                LOGGER.warn("Skipping entry with missing data: {}", tag);
                return null;
            }

            // Deserialize the ItemStack using Codec
            Tag stackTag = tag.get(KEY_STACK);
            if (stackTag == null) {
                LOGGER.warn("Skipping entry with missing stack tag");
                return null;
            }

            DataResult<ItemStack> parseResult = ItemStack.CODEC.parse(
                registryAccess.createSerializationContext(NbtOps.INSTANCE),
                stackTag
            );

            Optional<ItemStack> stackOpt = parseResult.result();
            if (stackOpt.isEmpty() || stackOpt.get().isEmpty()) {
                LOGGER.warn("Skipping entry with invalid or empty ItemStack");
                return null;
            }

            ItemStack stack = stackOpt.get();
            long count = tag.getLong(KEY_COUNT).orElse(0L);

            // Validate count
            if (count <= 0) {
                LOGGER.warn("Skipping entry with invalid count: {}", count);
                return null;
            }

            return new InfiniteInventoryEntry(stack, count);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize inventory entry, skipping: {}", tag, e);
            return null;
        }
    }

    /**
     * Serializes a complete InfiniteInventory to NBT.
     *
     * @param inventory The inventory to serialize
     * @param registryAccess Registry access for ItemStack serialization
     * @return CompoundTag containing the full inventory data
     */
    public static CompoundTag serialize(InfiniteInventory inventory, HolderLookup.Provider registryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_VERSION, VERSION);

        // Handle empty inventory
        if (inventory == null || inventory.isEmpty()) {
            tag.put(KEY_ITEMS, new ListTag());
            return tag;
        }

        // Serialize all entries
        ListTag itemsList = new ListTag();
        for (InfiniteInventoryEntry entry : inventory.getAllEntries()) {
            CompoundTag entryTag = writeEntry(entry, registryAccess);
            if (entryTag != null) {
                itemsList.add(entryTag);
            }
        }

        tag.put(KEY_ITEMS, itemsList);
        return tag;
    }

    /**
     * Deserializes a complete InfiniteInventory from NBT.
     * Handles missing or corrupted data gracefully by logging warnings and skipping bad entries.
     *
     * @param tag The tag to read from
     * @param registryAccess Registry access for ItemStack deserialization
     * @return A new InfiniteInventory with the deserialized data, or an empty inventory if tag is null/invalid
     */
    public static InfiniteInventory deserialize(CompoundTag tag, HolderLookup.Provider registryAccess) {
        InfiniteInventory inventory = new InfiniteInventory();

        if (tag == null) {
            LOGGER.warn("Attempted to deserialize null NBT tag, returning empty inventory");
            return inventory;
        }

        try {
            // Check version (for future migration support)
            int version = tag.getInt(KEY_VERSION).orElse(0);
            if (version > VERSION) {
                LOGGER.warn("Loading inventory data from newer version {} (current: {}). Some data may be lost.",
                           version, VERSION);
            } else if (version < VERSION && version > 0) {
                LOGGER.info("Loading inventory data from older version {} (current: {}). Applying migrations if needed.",
                           version, VERSION);
                // Future: Add migration logic here
            }

            // Deserialize items
            if (!tag.contains(KEY_ITEMS)) {
                LOGGER.warn("No items list found in NBT, returning empty inventory");
                return inventory;
            }

            ListTag itemsList = tag.getList(KEY_ITEMS).orElse(new ListTag());
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < itemsList.size(); i++) {
                Optional<CompoundTag> entryTagOpt = itemsList.getCompound(i);
                if (entryTagOpt.isEmpty()) {
                    failCount++;
                    continue;
                }

                InfiniteInventoryEntry entry = readEntry(entryTagOpt.get(), registryAccess);

                if (entry != null) {
                    // Add directly to the inventory
                    inventory.addItem(entry.getReferenceStack(), entry.getCount());
                    successCount++;
                } else {
                    failCount++;
                }
            }

            if (failCount > 0) {
                LOGGER.warn("Successfully loaded {} entries, failed to load {} entries", successCount, failCount);
            } else {
                LOGGER.debug("Successfully loaded {} inventory entries", successCount);
            }

        } catch (Exception e) {
            LOGGER.error("Error deserializing inventory, returning partially loaded data", e);
        }

        return inventory;
    }
}
