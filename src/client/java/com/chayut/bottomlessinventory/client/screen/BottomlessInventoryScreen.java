package com.chayut.bottomlessinventory.client.screen;

import com.chayut.bottomlessinventory.client.screen.widget.InfiniteGridWidget;
import com.chayut.bottomlessinventory.client.screen.widget.RecipeBookButtonWidget;
import com.chayut.bottomlessinventory.screen.BottomlessScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the bottomless inventory.
 * Displays a two-panel layout:
 * - Left panel: Armor, offhand, 2x2 crafting grid, recipe book
 * - Right panel: Infinite inventory grid (placeholder for now)
 * - Bottom: Hotbar
 */
public class BottomlessInventoryScreen extends AbstractContainerScreen<BottomlessScreenHandler> {

    // Screen dimensions - wider than vanilla to accommodate both panels
    private static final int BACKGROUND_WIDTH = 276;
    private static final int BACKGROUND_HEIGHT = 166;

    // Panel boundaries (for rendering placeholders)
    private static final int LEFT_PANEL_WIDTH = 176;
    private static final int RIGHT_PANEL_X_OFFSET = LEFT_PANEL_WIDTH;
    private static final int RIGHT_PANEL_WIDTH = BACKGROUND_WIDTH - LEFT_PANEL_WIDTH;

    // Color constants for placeholder rendering
    private static final int RIGHT_PANEL_COLOR = 0xFF7A7A7A;     // Slightly darker gray for right panel
    private static final int BORDER_COLOR = 0xFF3C3C3C;          // Dark border color

    // Vanilla inventory texture
    private static final ResourceLocation INVENTORY_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

    // Widget references
    private InfiniteGridWidget gridWidget;
    private RecipeBookButtonWidget recipeBookButton;

    public BottomlessInventoryScreen(BottomlessScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = BACKGROUND_WIDTH;
        this.imageHeight = BACKGROUND_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        // Initialize the infinite grid widget in the right panel
        int gridX = this.leftPos + RIGHT_PANEL_X_OFFSET + 8;
        int gridY = this.topPos + 30; // Leave space for tabs and search bar
        int gridWidth = RIGHT_PANEL_WIDTH - 16;
        int gridHeight = BACKGROUND_HEIGHT - 40; // Leave space at top and bottom

        this.gridWidget = new InfiniteGridWidget(gridX, gridY, gridWidth, gridHeight);
        this.addRenderableWidget(this.gridWidget);

        // Add recipe book button in the left panel
        // Position it near where the 2x2 crafting grid would be
        // Standard vanilla position is around x=104, y=61 from the screen origin
        int recipeBookX = this.leftPos + 104;
        int recipeBookY = this.topPos + 61;
        this.recipeBookButton = new RecipeBookButtonWidget(recipeBookX, recipeBookY);
        this.addRenderableWidget(this.recipeBookButton);

        // Future widget initialization will be added here:
        // - tabs, search bar, filter buttons
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // For now, use a simple gray background for the left panel
        // TODO: In the future, use vanilla inventory texture properly
        graphics.fill(x, y, x + LEFT_PANEL_WIDTH, y + BACKGROUND_HEIGHT, 0xFFC6C6C6);

        // Draw right panel background (infinite inventory area)
        graphics.fill(x + RIGHT_PANEL_X_OFFSET, y, x + BACKGROUND_WIDTH, y + BACKGROUND_HEIGHT, RIGHT_PANEL_COLOR);

        // Draw borders
        // Middle divider between left and right panels
        graphics.fill(x + LEFT_PANEL_WIDTH, y, x + LEFT_PANEL_WIDTH + 1, y + BACKGROUND_HEIGHT, BORDER_COLOR);
        // Right border
        graphics.fill(x + BACKGROUND_WIDTH - 1, y, x + BACKGROUND_WIDTH, y + BACKGROUND_HEIGHT, BORDER_COLOR);

        // Draw hotbar slot backgrounds
        // The hotbar slots are at indices 10-18, positioned at y=142 relative to topPos
        // Each slot is 18x18 pixels, starting at x=8
        renderHotbarSlotBackgrounds(graphics, x, y);

        // Draw armor slot backgrounds (4 slots: head, chest, legs, feet)
        renderArmorSlotBackgrounds(graphics, x, y);

        // Draw offhand slot background
        renderOffhandSlotBackground(graphics, x, y);

        // Draw crafting grid slot backgrounds (2x2 grid)
        renderCraftingGridSlotBackgrounds(graphics, x, y);

        // Draw crafting result slot background
        renderCraftingResultSlotBackground(graphics, x, y);
    }

    /**
     * Renders the background for hotbar slots.
     * Creates a visual appearance similar to vanilla inventory slots.
     */
    private void renderHotbarSlotBackgrounds(GuiGraphics graphics, int screenX, int screenY) {
        // Hotbar position: 9 slots starting at x=8, y=142
        int hotbarY = screenY + 142;

        for (int i = 0; i < 9; i++) {
            int slotX = screenX + 8 + i * 18;
            renderSlotBackground(graphics, slotX, hotbarY);
        }
    }

    /**
     * Renders the background for armor slots.
     * 4 slots at x=8, y=8/26/44/62
     */
    private void renderArmorSlotBackgrounds(GuiGraphics graphics, int screenX, int screenY) {
        for (int i = 0; i < 4; i++) {
            int slotX = screenX + 8;
            int slotY = screenY + 8 + i * 18;
            renderSlotBackground(graphics, slotX, slotY);
        }
    }

    /**
     * Renders the background for the offhand slot.
     * 1 slot at x=77, y=62
     */
    private void renderOffhandSlotBackground(GuiGraphics graphics, int screenX, int screenY) {
        int slotX = screenX + 77;
        int slotY = screenY + 62;
        renderSlotBackground(graphics, slotX, slotY);
    }

    /**
     * Renders the background for crafting grid slots.
     * 4 slots (2x2) starting at x=98, y=18
     */
    private void renderCraftingGridSlotBackgrounds(GuiGraphics graphics, int screenX, int screenY) {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int slotX = screenX + 98 + col * 18;
                int slotY = screenY + 18 + row * 18;
                renderSlotBackground(graphics, slotX, slotY);
            }
        }
    }

    /**
     * Renders the background for the crafting result slot.
     * 1 slot at x=154, y=28
     */
    private void renderCraftingResultSlotBackground(GuiGraphics graphics, int screenX, int screenY) {
        int slotX = screenX + 154;
        int slotY = screenY + 28;
        renderSlotBackground(graphics, slotX, slotY);
    }

    /**
     * Renders a single slot background with 3D border effect.
     * Creates a visual appearance similar to vanilla inventory slots.
     */
    private void renderSlotBackground(GuiGraphics graphics, int slotX, int slotY) {
        // Draw slot background (darker gray for slot interior)
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);

        // Draw slot border (lighter on top-left, darker on bottom-right for 3D effect)
        // Top border (light)
        graphics.fill(slotX - 1, slotY - 1, slotX + 16, slotY, 0xFFFFFFFF);
        // Left border (light)
        graphics.fill(slotX - 1, slotY - 1, slotX, slotY + 16, 0xFFFFFFFF);
        // Bottom border (dark)
        graphics.fill(slotX, slotY + 16, slotX + 17, slotY + 17, 0xFF373737);
        // Right border (dark)
        graphics.fill(slotX + 16, slotY, slotX + 17, slotY + 17, 0xFF373737);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Render the background (darkened overlay behind the UI)
        this.renderBackground(graphics, mouseX, mouseY, delta);

        // Render the screen background and slots
        super.render(graphics, mouseX, mouseY, delta);

        // Render item tooltips
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw the screen title - positioned in the left panel
        graphics.drawString(this.font, this.title, 8, 6, 4210752, false);

        // Draw placeholder text in the right panel to indicate where the infinite grid will go
        Component gridLabel = Component.literal("Infinite Inventory Grid");
        int labelX = LEFT_PANEL_WIDTH + 10;
        int labelY = 10;
        graphics.drawString(this.font, gridLabel, labelX, labelY, 0xFFFFFF, false);

        // Player inventory label is not drawn - the hotbar is part of the bottomless inventory
    }
}
