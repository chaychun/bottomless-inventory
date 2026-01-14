package com.chayut.bottomlessinventory.screen;

import com.chayut.bottomlessinventory.BottomlessInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the bottomless inventory screen.
 * Includes slots for armor, offhand, hotbar, and a 2x2 crafting grid.
 * The infinite inventory grid is rendered virtually on the client side (no Slot objects).
 */
public class BottomlessScreenHandler extends AbstractContainerMenu {

    // Crafting grid and result holder
    private final CraftingContainer craftingContainer;
    private final ResultContainer resultContainer;
    private final ContainerLevelAccess access;
    private final Player player;

    // Slot indices for easy reference
    public static final int CRAFTING_RESULT_SLOT = 0;
    public static final int CRAFTING_INPUT_START = 1;
    public static final int CRAFTING_INPUT_END = 4;
    public static final int ARMOR_START = 5;
    public static final int ARMOR_END = 8;
    public static final int OFFHAND_SLOT = 9;
    public static final int HOTBAR_START = 10;
    public static final int HOTBAR_END = 18;

    /**
     * Client-side constructor (no container level access).
     */
    public BottomlessScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    /**
     * Server-side constructor with container level access.
     */
    public BottomlessScreenHandler(int syncId, Inventory playerInventory, ContainerLevelAccess access) {
        super(BottomlessInventory.BOTTOMLESS_SCREEN_HANDLER_TYPE, syncId);

        this.player = playerInventory.player;
        this.access = access;
        this.craftingContainer = new TransientCraftingContainer(this, 2, 2);
        this.resultContainer = new ResultContainer();

        // Add crafting result slot (index 0)
        this.addSlot(new ResultSlot(playerInventory.player, this.craftingContainer, this.resultContainer, 0, 154, 28));

        // Add 2x2 crafting grid (indices 1-4)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 2; ++col) {
                this.addSlot(new Slot(this.craftingContainer, col + row * 2, 98 + col * 18, 18 + row * 18));
            }
        }

        // Add armor slots (indices 5-8)
        // Order: head, chest, legs, feet (reverse of vanilla inventory)
        for (int i = 0; i < 4; ++i) {
            final int armorSlotIndex = 39 - i; // Vanilla armor slots: 36=feet, 37=legs, 38=chest, 39=head
            this.addSlot(new Slot(playerInventory, armorSlotIndex, 8, 8 + i * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true; // Simplified for now - will validate via equipment slot type
                }
            });
        }

        // Add offhand slot (index 9)
        this.addSlot(new Slot(playerInventory, 40, 77, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });

        // Add hotbar slots (indices 10-18)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    /**
     * Called when the crafting grid changes.
     * Recalculates the crafting result.
     */
    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        // Crafting result calculation will be implemented in Phase 4
        // For now, leave empty to allow compilation
        // The vanilla inventory screen uses InventoryMenu which handles crafting internally
    }

    /**
     * Called when a slot is clicked.
     * Handles special cases like crafting result slot.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // Crafting result slot
            if (slotIndex == CRAFTING_RESULT_SLOT) {
                this.access.execute((level, pos) -> slotStack.getItem().onCraftedBy(slotStack, player));

                if (!this.moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(slotStack, result);
            }
            // Crafting input slots
            else if (slotIndex >= CRAFTING_INPUT_START && slotIndex <= CRAFTING_INPUT_END) {
                if (!this.moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Armor slots
            else if (slotIndex >= ARMOR_START && slotIndex <= ARMOR_END) {
                if (!this.moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Offhand slot
            else if (slotIndex == OFFHAND_SLOT) {
                if (!this.moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Hotbar slots
            else if (slotIndex >= HOTBAR_START && slotIndex <= HOTBAR_END) {
                // Simplified for now - just fail the quick move from hotbar
                // TODO: Implement proper armor/offhand detection in Phase 4
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);

            if (slotIndex == CRAFTING_RESULT_SLOT) {
                player.drop(slotStack, false);
            }
        }

        return result;
    }

    /**
     * Checks if the player can still interact with this screen handler.
     */
    @Override
    public boolean stillValid(Player player) {
        return true; // Always valid since it's the player's own inventory
    }

    /**
     * Called when the screen handler is closed.
     * Drops items from the crafting grid.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            this.clearContainer(player, this.craftingContainer);
        });
    }

    /**
     * Gets the crafting container for this screen handler.
     */
    public CraftingContainer getCraftingContainer() {
        return this.craftingContainer;
    }

    /**
     * Gets the result container for this screen handler.
     */
    public ResultContainer getResultContainer() {
        return this.resultContainer;
    }
}
