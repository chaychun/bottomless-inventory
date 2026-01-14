package com.chayut.bottomlessinventory.network;

import com.chayut.bottomlessinventory.BottomlessInventory;
import com.chayut.bottomlessinventory.data.ModAttachments;
import com.chayut.bottomlessinventory.inventory.InfiniteInventory;
import com.chayut.bottomlessinventory.network.packets.InventoryActionPacket;
import com.chayut.bottomlessinventory.network.packets.SyncInventoryPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles inventory synchronization between server and clients.
 * Manages full syncs on player events and incremental syncs on changes.
 * Also processes client action requests with rate limiting.
 */
public class InventorySyncHandler {

    /**
     * Minimum interval between actions from the same player (in milliseconds).
     * 50ms = max 20 actions per second.
     */
    private static final long MIN_ACTION_INTERVAL_MS = 50;

    /**
     * Tracks the last action timestamp for each player for rate limiting.
     */
    private static final Map<UUID, Long> lastActionTime = new ConcurrentHashMap<>();

    /**
     * Registers all event handlers and packet receivers.
     * Should be called from BottomlessInventory.onInitialize() AFTER BottomlessNetworking.register().
     */
    public static void register() {
        BottomlessInventory.LOGGER.info("Registering Bottomless Inventory sync handlers");

        // Register player join event - send full sync
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            sendFullSync(player);
            BottomlessInventory.LOGGER.debug("Sent full inventory sync to {} on join", player.getName().getString());
        });

        // Register player respawn event - send full sync to new player entity
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            sendFullSync(newPlayer);
            BottomlessInventory.LOGGER.debug("Sent full inventory sync to {} after respawn", newPlayer.getName().getString());
        });

        // Note: Dimension change syncing would require a mixin since Fabric API doesn't
        // provide a built-in AFTER_CHANGE_DIMENSION event. For now, the copyOnDeath()
        // attachment behavior and respawn sync should handle most dimension-related scenarios.
        // A future improvement could add a mixin to detect ServerPlayer.changeDimension calls.

        // Register packet receiver for client actions
        ServerPlayNetworking.registerGlobalReceiver(InventoryActionPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            handleActionPacket(player, payload);
        });

        BottomlessInventory.LOGGER.info("Bottomless Inventory sync handlers registered successfully");
    }

    // === Full Sync Methods ===

    /**
     * Sends a full inventory sync to the specified player.
     * This replaces the client's entire cached inventory.
     *
     * @param player The player to sync to
     */
    public static void sendFullSync(ServerPlayer player) {
        if (player == null) {
            return;
        }

        InfiniteInventory inventory = ModAttachments.getInventory(player);
        SyncInventoryPacket packet = SyncInventoryPacket.fullSync(inventory);
        ServerPlayNetworking.send(player, packet);
    }

    // === Incremental Sync Methods ===

    /**
     * Sends an incremental sync for a single item change.
     * Use this when an item's count has changed.
     *
     * @param player The player to sync to
     * @param stack The item that changed (used as template/key)
     * @param newCount The new count of the item
     */
    public static void syncItemChange(ServerPlayer player, ItemStack stack, long newCount) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }

        SyncInventoryPacket packet = SyncInventoryPacket.incrementalSync(stack, newCount);
        ServerPlayNetworking.send(player, packet);
    }

    /**
     * Sends an incremental sync for an item removal.
     * This is a convenience method that sends count=0.
     *
     * @param player The player to sync to
     * @param stack The item that was removed
     */
    public static void syncItemRemoval(ServerPlayer player, ItemStack stack) {
        syncItemChange(player, stack, 0);
    }

    // === Action Packet Handling ===

    /**
     * Handles an incoming action packet from a client.
     * Validates the packet, checks rate limiting, and processes the action.
     *
     * @param player The player who sent the packet
     * @param packet The action packet to process
     */
    private static void handleActionPacket(ServerPlayer player, InventoryActionPacket packet) {
        // Validate packet
        if (!packet.isValid()) {
            BottomlessInventory.LOGGER.warn("Received invalid action packet from {}: {}",
                    player.getName().getString(), packet);
            return;
        }

        // Check rate limit
        if (!checkRateLimit(player)) {
            BottomlessInventory.LOGGER.warn("Rate limiting player {} for excessive inventory actions",
                    player.getName().getString());
            return;
        }

        // Process based on action type
        switch (packet.actionType()) {
            case TAKE_ITEMS -> handleTakeItems(player, packet.targetStack(), packet.amount());
            case DEPOSIT_ITEMS -> handleDepositItems(player, packet.targetStack(), packet.amount());
            case QUICK_MOVE -> handleQuickMove(player, packet.targetStack(), packet.amount());
        }
    }

    /**
     * Handles a TAKE_ITEMS action - removes items from infinite inventory and gives to player.
     *
     * @param player The player taking items
     * @param stack The item type to take
     * @param amount The amount requested
     */
    private static void handleTakeItems(ServerPlayer player, ItemStack stack, long amount) {
        InfiniteInventory inventory = ModAttachments.getInventory(player);

        // Check how many are available
        long available = inventory.getCount(stack);
        if (available <= 0) {
            BottomlessInventory.LOGGER.debug("Player {} tried to take items not in inventory: {}",
                    player.getName().getString(), stack.getItem());
            return;
        }

        // Calculate how many to actually give (respect max stack size and available amount)
        int maxStackSize = stack.getMaxStackSize();
        long toGive = Math.min(amount, available);

        // Give items to player in stack-sized chunks
        long totalGiven = 0;
        Inventory playerInv = player.getInventory();

        while (totalGiven < toGive) {
            int chunkSize = (int) Math.min(maxStackSize, toGive - totalGiven);
            ItemStack giveStack = stack.copyWithCount(chunkSize);

            // Try to add to player inventory
            if (!playerInv.add(giveStack)) {
                // Inventory full - stop giving items
                BottomlessInventory.LOGGER.debug("Player {} inventory full while taking items", player.getName().getString());
                break;
            }

            totalGiven += chunkSize;
        }

        // Remove the items we actually gave from infinite inventory
        if (totalGiven > 0) {
            inventory.removeItem(stack, totalGiven);
            ModAttachments.setInventory(player, inventory);

            // Send incremental sync
            long newCount = inventory.getCount(stack);
            syncItemChange(player, stack, newCount);

            BottomlessInventory.LOGGER.debug("Player {} took {} {} from infinite inventory",
                    player.getName().getString(), totalGiven, stack.getItem());
        }
    }

    /**
     * Handles a DEPOSIT_ITEMS action - takes items from player and adds to infinite inventory.
     *
     * @param player The player depositing items
     * @param stack The item type to deposit
     * @param amount The amount to deposit
     */
    private static void handleDepositItems(ServerPlayer player, ItemStack stack, long amount) {
        InfiniteInventory inventory = ModAttachments.getInventory(player);
        Inventory playerInv = player.getInventory();

        // Count how many matching items the player has
        long playerHas = countMatchingItems(playerInv, stack);
        if (playerHas <= 0) {
            BottomlessInventory.LOGGER.debug("Player {} tried to deposit items they don't have: {}",
                    player.getName().getString(), stack.getItem());
            return;
        }

        // Calculate how many to actually take
        long toTake = Math.min(amount, playerHas);
        long totalTaken = 0;

        // Remove items from player inventory
        for (int i = 0; i < playerInv.getContainerSize() && totalTaken < toTake; i++) {
            ItemStack slotStack = playerInv.getItem(i);
            if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int canTake = (int) Math.min(slotStack.getCount(), toTake - totalTaken);
                slotStack.shrink(canTake);
                totalTaken += canTake;

                if (slotStack.isEmpty()) {
                    playerInv.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        // Also check cursor slot (carried item)
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, stack) && totalTaken < toTake) {
            int canTake = (int) Math.min(carried.getCount(), toTake - totalTaken);
            carried.shrink(canTake);
            totalTaken += canTake;

            if (carried.isEmpty()) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
        }

        // Add to infinite inventory
        if (totalTaken > 0) {
            inventory.addItem(stack, totalTaken);
            ModAttachments.setInventory(player, inventory);

            // Send incremental sync
            long newCount = inventory.getCount(stack);
            syncItemChange(player, stack, newCount);

            BottomlessInventory.LOGGER.debug("Player {} deposited {} {} to infinite inventory",
                    player.getName().getString(), totalTaken, stack.getItem());
        }
    }

    /**
     * Handles a QUICK_MOVE action - similar to shift-click, moves items between inventories.
     * If item exists in infinite inventory, takes items to player.
     * If item exists in player inventory, deposits to infinite inventory.
     *
     * @param player The player performing quick move
     * @param stack The item type to move
     * @param amount The amount to move
     */
    private static void handleQuickMove(ServerPlayer player, ItemStack stack, long amount) {
        InfiniteInventory inventory = ModAttachments.getInventory(player);

        // Check if player has this item - if so, deposit
        long playerHas = countMatchingItems(player.getInventory(), stack);
        if (playerHas > 0) {
            handleDepositItems(player, stack, amount);
            return;
        }

        // Otherwise, try to take from infinite inventory
        long infiniteHas = inventory.getCount(stack);
        if (infiniteHas > 0) {
            handleTakeItems(player, stack, amount);
            return;
        }

        BottomlessInventory.LOGGER.debug("Player {} quick move had no items to move: {}",
                player.getName().getString(), stack.getItem());
    }

    // === Rate Limiting ===

    /**
     * Checks if a player is within the rate limit for actions.
     * Updates the last action time if allowed.
     *
     * @param player The player to check
     * @return true if the action is allowed, false if rate limited
     */
    static boolean checkRateLimit(ServerPlayer player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        Long lastTime = lastActionTime.get(playerId);
        if (lastTime != null && now - lastTime < MIN_ACTION_INTERVAL_MS) {
            return false;
        }

        lastActionTime.put(playerId, now);
        return true;
    }

    /**
     * Clears rate limit tracking for a player.
     * Should be called when a player disconnects to prevent memory leaks.
     *
     * @param playerId The UUID of the player to clear
     */
    public static void clearRateLimitTracking(UUID playerId) {
        lastActionTime.remove(playerId);
    }

    /**
     * Gets the minimum action interval for rate limiting (for testing).
     *
     * @return The minimum interval in milliseconds
     */
    static long getMinActionIntervalMs() {
        return MIN_ACTION_INTERVAL_MS;
    }

    // === Helper Methods ===

    /**
     * Counts how many items matching the given stack the player has in their inventory.
     *
     * @param playerInv The player's inventory
     * @param stack The item to match
     * @return The total count of matching items
     */
    private static long countMatchingItems(Inventory playerInv, ItemStack stack) {
        long count = 0;
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack slotStack = playerInv.getItem(i);
            if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }
}
