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
