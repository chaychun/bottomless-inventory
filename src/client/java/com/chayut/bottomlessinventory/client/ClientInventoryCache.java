package com.chayut.bottomlessinventory.client;

import com.chayut.bottomlessinventory.inventory.ItemStackKey;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket.SyncEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Client-side cache for the infinite inventory system.
 * Receives sync packets from the server and maintains a local copy of the inventory state.
 * This cache is used by the GUI to display inventory contents without server round-trips.
 */
public class ClientInventoryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInventoryCache.class);

    private static ClientInventoryCache INSTANCE;

    private final Map<ItemStackKey, CachedEntry> cache;

    /**
     * A cached entry containing the reference ItemStack and its count.
     */
    public static class CachedEntry {
        private final ItemStack referenceStack;
        private long count;

        /**
         * Creates a new cached entry.
         *
         * @param referenceStack The reference ItemStack (will be copied)
         * @param count          The count of items
         */
        public CachedEntry(ItemStack referenceStack, long count) {
            // Store a copy with count of 1 as the reference
            this.referenceStack = referenceStack.copy();
            this.referenceStack.setCount(1);
            this.count = count;
        }

        /**
         * Gets a copy of the reference ItemStack.
         *
         * @return A copy of the reference stack with count 1
         */
        public ItemStack getReferenceStack() {
            return referenceStack.copy();
        }

        /**
         * Gets the count of items stored.
         *
         * @return The count
         */
        public long getCount() {
            return count;
        }

        /**
         * Sets the count of items stored.
         *
         * @param count The new count
         */
        void setCount(long count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "CachedEntry{" +
                    "item=" + BuiltInRegistries.ITEM.getKey(referenceStack.getItem()) +
                    ", count=" + count +
                    '}';
        }
    }

    /**
     * Private constructor for singleton pattern.
     */
    private ClientInventoryCache() {
        this.cache = new HashMap<>();
    }

    /**
     * Gets the singleton instance of the client inventory cache.
     *
     * @return The cache instance
     */
    public static ClientInventoryCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClientInventoryCache();
        }
        return INSTANCE;
    }

    /**
     * Resets the singleton instance.
     * Primarily used for testing.
     */
    public static void resetInstance() {
        INSTANCE = null;
    }

    /**
     * Registers the client-side packet receiver for inventory sync packets.
     * Should be called from BottomlessInventoryClient.onInitializeClient().
     */
    public static void register() {
        LOGGER.info("Registering client inventory cache packet receiver");

        ClientPlayNetworking.registerGlobalReceiver(SyncInventoryPacket.TYPE, (payload, context) -> {
            // Process sync on main thread for thread safety
            context.client().execute(() -> {
                ClientInventoryCache.getInstance().handleSync(payload);
            });
        });

        LOGGER.info("Client inventory cache packet receiver registered successfully");
    }

    /**
     * Handles a sync packet from the server.
     * Must be called on the client main thread.
     *
     * @param packet The sync packet to process
     */
    public void handleSync(SyncInventoryPacket packet) {
        if (!packet.isCompatibleVersion()) {
            LOGGER.warn("Received incompatible sync packet version: {} (expected: {})",
                    packet.version(), SyncInventoryPacket.PACKET_VERSION);
            return;
        }

        switch (packet.syncType()) {
            case FULL -> handleFullSync(packet);
            case INCREMENTAL -> handleIncrementalSync(packet);
        }
    }

    /**
     * Handles a full sync - clears the cache and replaces with new entries.
     *
     * @param packet The full sync packet
     */
    private void handleFullSync(SyncInventoryPacket packet) {
        cache.clear();

        for (SyncEntry entry : packet.entries()) {
            if (entry.stack() == null || entry.stack().isEmpty()) {
                continue;
            }
            ItemStackKey key = new ItemStackKey(entry.stack());
            cache.put(key, new CachedEntry(entry.stack(), entry.count()));
        }

        LOGGER.debug("Full sync completed: {} unique items", cache.size());
    }

    /**
     * Handles an incremental sync - updates/adds entries, removes entries with count 0.
     *
     * @param packet The incremental sync packet
     */
    private void handleIncrementalSync(SyncInventoryPacket packet) {
        for (SyncEntry entry : packet.entries()) {
            if (entry.stack() == null || entry.stack().isEmpty()) {
                continue;
            }

            ItemStackKey key = new ItemStackKey(entry.stack());

            if (entry.count() <= 0) {
                // Remove entries with count 0 or less
                cache.remove(key);
                LOGGER.debug("Removed item from cache: {}", key);
            } else {
                // Update or add entry
                CachedEntry existing = cache.get(key);
                if (existing != null) {
                    existing.setCount(entry.count());
                    LOGGER.debug("Updated item in cache: {} -> {}", key, entry.count());
                } else {
                    cache.put(key, new CachedEntry(entry.stack(), entry.count()));
                    LOGGER.debug("Added item to cache: {} = {}", key, entry.count());
                }
            }
        }

        LOGGER.debug("Incremental sync completed: {} entries processed, {} unique items in cache",
                packet.entries().size(), cache.size());
    }

    // === Query Methods ===

    /**
     * Gets the count of a specific item type in the cache.
     *
     * @param stack The ItemStack to check (used as key)
     * @return The count of matching items, or 0 if not present
     */
    public long getCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        ItemStackKey key = new ItemStackKey(stack);
        CachedEntry entry = cache.get(key);
        return entry != null ? entry.getCount() : 0;
    }

    /**
     * Gets all entries in the cache.
     * The returned collection is a copy and can be safely iterated.
     *
     * @return A collection of all cached entries
     */
    public Collection<CachedEntry> getAllEntries() {
        return new ArrayList<>(cache.values());
    }

    /**
     * Checks if the cache contains a specific item type.
     *
     * @param stack The ItemStack to check
     * @return true if at least one of this item type is cached
     */
    public boolean contains(ItemStack stack) {
        return getCount(stack) > 0;
    }

    /**
     * Gets the number of unique item types in the cache.
     *
     * @return The number of unique item types
     */
    public int getTotalUniqueItems() {
        return cache.size();
    }

    /**
     * Checks if the cache is empty.
     *
     * @return true if no items are cached
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * Gets entries sorted by item registry name for consistent display ordering.
     * This is useful for GUI rendering to ensure items appear in a predictable order.
     *
     * @return A list of entries sorted by item registry name
     */
    public List<CachedEntry> getSortedEntries() {
        List<CachedEntry> entries = new ArrayList<>(cache.values());

        entries.sort((a, b) -> {
            String nameA = BuiltInRegistries.ITEM.getKey(a.referenceStack.getItem()).toString();
            String nameB = BuiltInRegistries.ITEM.getKey(b.referenceStack.getItem()).toString();
            return nameA.compareTo(nameB);
        });

        return entries;
    }

    /**
     * Clears the cache.
     * Primarily used for testing or when disconnecting from a server.
     */
    public void clear() {
        cache.clear();
        LOGGER.debug("Cache cleared");
    }

    @Override
    public String toString() {
        return "ClientInventoryCache{" +
                "uniqueItems=" + cache.size() +
                '}';
    }
}
