package com.chayut.bottomlessinventory.client.screen.widget;

import com.chayut.bottomlessinventory.client.ClientInventoryCache;
import com.chayut.bottomlessinventory.client.ClientInventoryCache.CachedEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrollable grid widget that displays items from the infinite inventory.
 * Features:
 * - 9-column grid layout matching creative mode inventory
 * - Scrolling support with mouse wheel
 * - Click handling for item interactions
 * - Abbreviated count display with full count in tooltip
 */
public class InfiniteGridWidget extends AbstractWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfiniteGridWidget.class);

    // Grid layout constants
    private static final int COLUMNS = 9;
    private static final int CELL_SIZE = 18;
    private static final int ITEM_SIZE = 16;
    private static final int CELL_PADDING = 1; // Padding inside each cell for the item icon

    // Scroll state
    private int scrollOffset = 0; // Current row at the top

    // Color constants
    private static final int SLOT_COLOR = 0xFF8B8B8B;
    private static final int SLOT_BORDER_COLOR = 0xFF373737;
    private static final int HOVER_HIGHLIGHT_COLOR = 0x80FFFFFF;
    private static final int COUNT_COLOR = 0xFFFFFF;
    private static final int COUNT_SHADOW_COLOR = 0x3F3F3F;

    /**
     * Creates a new infinite grid widget.
     *
     * @param x      The x position
     * @param y      The y position
     * @param width  The width of the widget
     * @param height The height of the widget
     */
    public InfiniteGridWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Infinite Grid"));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Get items from cache
        List<CachedEntry> entries = ClientInventoryCache.getInstance().getSortedEntries();

        // Calculate grid dimensions
        int visibleRows = getVisibleRows();
        int totalRows = getTotalRows(entries.size());

        // Render grid cells
        for (int row = 0; row < visibleRows; row++) {
            int dataRow = row + scrollOffset;
            if (dataRow >= totalRows) {
                break;
            }

            for (int col = 0; col < COLUMNS; col++) {
                int index = dataRow * COLUMNS + col;
                if (index >= entries.size()) {
                    break;
                }

                CachedEntry entry = entries.get(index);
                renderCell(graphics, row, col, entry, mouseX, mouseY);
            }
        }
    }

    /**
     * Renders a single cell in the grid.
     *
     * @param graphics The graphics context
     * @param row      The visual row (0-based from top of widget)
     * @param col      The column (0-based from left)
     * @param entry    The cached entry to render
     * @param mouseX   The mouse X position
     * @param mouseY   The mouse Y position
     */
    private void renderCell(GuiGraphics graphics, int row, int col, CachedEntry entry, int mouseX, int mouseY) {
        int cellX = getX() + col * CELL_SIZE;
        int cellY = getY() + row * CELL_SIZE;

        // Draw slot background
        graphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, SLOT_COLOR);

        // Draw slot border
        graphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + 1, SLOT_BORDER_COLOR); // Top
        graphics.fill(cellX, cellY + CELL_SIZE - 1, cellX + CELL_SIZE, cellY + CELL_SIZE, SLOT_BORDER_COLOR); // Bottom
        graphics.fill(cellX, cellY, cellX + 1, cellY + CELL_SIZE, SLOT_BORDER_COLOR); // Left
        graphics.fill(cellX + CELL_SIZE - 1, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, SLOT_BORDER_COLOR); // Right

        // Check if mouse is hovering over this cell
        boolean isHovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE &&
                           mouseY >= cellY && mouseY < cellY + CELL_SIZE;

        // Draw hover highlight
        if (isHovered) {
            graphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, HOVER_HIGHLIGHT_COLOR);
        }

        // Render item icon centered in cell
        ItemStack stack = entry.getReferenceStack();
        int itemX = cellX + CELL_PADDING;
        int itemY = cellY + CELL_PADDING;
        graphics.renderItem(stack, itemX, itemY);

        // Render count overlay (bottom-right)
        String countText = abbreviateCount(entry.getCount());
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(countText);
        int textX = cellX + CELL_SIZE - 2 - textWidth; // Right-aligned
        int textY = cellY + CELL_SIZE - 9; // Bottom-aligned (font height is ~9px)

        // Draw shadow first
        graphics.drawString(font, countText, textX + 1, textY + 1, COUNT_SHADOW_COLOR, false);
        // Draw text
        graphics.drawString(font, countText, textX, textY, COUNT_COLOR, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        // Calculate which cell was clicked
        int relativeX = (int) (mouseX - getX());
        int relativeY = (int) (mouseY - getY());

        int col = relativeX / CELL_SIZE;
        int row = relativeY / CELL_SIZE;

        if (col < 0 || col >= COLUMNS) {
            return false;
        }

        int dataRow = row + scrollOffset;
        int index = dataRow * COLUMNS + col;

        List<CachedEntry> entries = ClientInventoryCache.getInstance().getSortedEntries();
        if (index < 0 || index >= entries.size()) {
            return false;
        }

        CachedEntry entry = entries.get(index);
        handleCellClick(entry, button, hasShiftDown());

        return true;
    }

    /**
     * Handles a click on a cell.
     *
     * @param entry       The entry that was clicked
     * @param button      The mouse button (0 = left, 1 = right)
     * @param shiftDown   Whether shift is held
     */
    private void handleCellClick(CachedEntry entry, int button, boolean shiftDown) {
        ItemStack stack = entry.getReferenceStack();
        long count = entry.getCount();

        if (shiftDown) {
            // Shift-click: Move one stack (max 64) to hotbar
            int amount = (int) Math.min(64, count);
            LOGGER.info("SHIFT-CLICK: Would move {} x{} to hotbar (sending network packet)",
                    stack.getHoverName().getString(), amount);
            // TODO: Send network packet to server
        } else if (button == 0) {
            // Left-click: Pick up full stack (max 64)
            int amount = (int) Math.min(64, count);
            LOGGER.info("LEFT-CLICK: Would pick up {} x{} (sending network packet)",
                    stack.getHoverName().getString(), amount);
            // TODO: Send network packet to server
        } else if (button == 1) {
            // Right-click: Pick up half stack
            int amount = (int) Math.min(32, count / 2);
            if (amount == 0 && count > 0) {
                amount = 1; // Always pick up at least 1 if available
            }
            LOGGER.info("RIGHT-CLICK: Would pick up {} x{} (sending network packet)",
                    stack.getHoverName().getString(), amount);
            // TODO: Send network packet to server
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        // Scroll up (positive) or down (negative)
        int scrollDelta = scrollY > 0 ? -1 : 1;
        int newOffset = scrollOffset + scrollDelta;

        // Calculate max scroll offset
        List<CachedEntry> entries = ClientInventoryCache.getInstance().getSortedEntries();
        int totalRows = getTotalRows(entries.size());
        int visibleRows = getVisibleRows();
        int maxOffset = Math.max(0, totalRows - visibleRows);

        // Clamp to valid range
        scrollOffset = Math.max(0, Math.min(newOffset, maxOffset));

        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal("Infinite Inventory Grid"));

        int totalItems = ClientInventoryCache.getInstance().getTotalUniqueItems();
        output.add(NarratedElementType.USAGE,
                Component.literal(totalItems + " unique items. Use mouse wheel to scroll."));
    }

    /**
     * Gets the number of rows that can be displayed in the widget.
     *
     * @return The number of visible rows
     */
    private int getVisibleRows() {
        return height / CELL_SIZE;
    }

    /**
     * Gets the total number of rows needed to display all items.
     *
     * @param itemCount The total number of items
     * @return The total number of rows
     */
    private int getTotalRows(int itemCount) {
        return (int) Math.ceil((double) itemCount / COLUMNS);
    }

    /**
     * Abbreviates a count for display.
     * Examples:
     * - 1,000 -> "1K"
     * - 10,000 -> "10K"
     * - 1,000,000 -> "1M"
     * - 1,000,000,000 -> "1B"
     *
     * @param count The count to abbreviate
     * @return The abbreviated count string
     */
    public static String abbreviateCount(long count) {
        if (count >= 1_000_000_000) {
            double value = count / 1_000_000_000.0;
            // Format with 1 decimal place, but remove trailing .0
            String formatted = String.format("%.1fB", value);
            return formatted.replace(".0B", "B");
        }
        if (count >= 1_000_000) {
            double value = count / 1_000_000.0;
            String formatted = String.format("%.1fM", value);
            return formatted.replace(".0M", "M");
        }
        if (count >= 1_000) {
            double value = count / 1_000.0;
            String formatted = String.format("%.1fK", value);
            return formatted.replace(".0K", "K");
        }
        return String.valueOf(count);
    }

    /**
     * Checks if shift is currently held down.
     *
     * @return true if shift is held
     */
    private boolean hasShiftDown() {
        return net.minecraft.client.Minecraft.getInstance().options.keyShift.isDown();
    }

    /**
     * Gets the current scroll offset (which row is at the top).
     *
     * @return The scroll offset in rows
     */
    public int getScrollOffset() {
        return scrollOffset;
    }

    /**
     * Sets the scroll offset.
     *
     * @param offset The new scroll offset in rows
     */
    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, offset);
    }
}
