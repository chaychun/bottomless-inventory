package com.chayut.bottomlessinventory.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A toggle button widget for the recipe book.
 * This is a placeholder button that toggles between active and inactive states.
 * Full recipe book integration will be implemented in a later phase.
 */
public class RecipeBookButtonWidget extends Button {
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 18;

    // Toggle state
    private boolean toggled = false;

    /**
     * Creates a new recipe book button widget.
     *
     * @param x The x position
     * @param y The y position
     */
    public RecipeBookButtonWidget(int x, int y) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.recipebook.toggleRecipes"),
              button -> ((RecipeBookButtonWidget) button).toggle(), Button.DEFAULT_NARRATION);
    }

    /**
     * Toggles the button state.
     */
    private void toggle() {
        this.toggled = !this.toggled;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();

        // Draw button background with 3D border effect that changes based on toggle state
        int bgColor = toggled ? 0xFF6B8E23 : 0xFF8B4513; // Green when active, brown when inactive

        // Background
        graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, bgColor);

        // 3D borders
        graphics.fill(x, y, x + BUTTON_WIDTH, y + 1, 0xFFFFFFFF); // Top (light)
        graphics.fill(x, y, x + 1, y + BUTTON_HEIGHT, 0xFFFFFFFF); // Left (light)
        graphics.fill(x, y + BUTTON_HEIGHT - 1, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0xFF373737); // Bottom (dark)
        graphics.fill(x + BUTTON_WIDTH - 1, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0xFF373737); // Right (dark)

        // Draw a simple book icon (rectangle with lines)
        int iconX = x + 4;
        int iconY = y + 3;
        // Book cover
        graphics.fill(iconX, iconY, iconX + 12, iconY + 12, 0xFFDEB887);
        // Book spine
        graphics.fill(iconX, iconY, iconX + 2, iconY + 12, 0xFF8B4513);
        // Page lines
        graphics.fill(iconX + 4, iconY + 3, iconX + 10, iconY + 4, 0xFF000000);
        graphics.fill(iconX + 4, iconY + 5, iconX + 10, iconY + 6, 0xFF000000);
        graphics.fill(iconX + 4, iconY + 7, iconX + 10, iconY + 8, 0xFF000000);

        // Draw hover overlay if mouse is over the button
        if (isHovered()) {
            graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0x40FFFFFF);
        }
    }

    /**
     * Gets the current toggle state.
     *
     * @return true if the button is toggled on, false otherwise
     */
    public boolean isToggled() {
        return toggled;
    }

    /**
     * Sets the toggle state.
     *
     * @param toggled The new toggle state
     */
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
}
