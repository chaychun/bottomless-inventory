package com.chayut.bottomlessinventory.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A toggle button widget for the recipe book.
 * This is a placeholder button that toggles between active and inactive states.
 * Full recipe book integration will be implemented in a later phase.
 */
public class RecipeBookButtonWidget extends AbstractWidget {
    // Vanilla recipe book button texture location
    private static final ResourceLocation RECIPE_BUTTON_LOCATION =
        ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");

    // Texture coordinates for the button states (in the recipe_book.png texture)
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 18;
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    // UV coordinates for button states
    private static final int INACTIVE_U = 0;
    private static final int INACTIVE_V = 2;
    private static final int ACTIVE_U = 0;
    private static final int ACTIVE_V = 20;

    // Toggle state
    private boolean toggled = false;

    /**
     * Creates a new recipe book button widget.
     *
     * @param x The x position
     * @param y The y position
     */
    public RecipeBookButtonWidget(int x, int y) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.recipebook.toggleRecipes"));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Draw button background with 3D border effect
        int x = getX();
        int y = getY();
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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        // Toggle the state when left-clicked
        if (button == 0) {
            this.toggled = !this.toggled;
            playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
            return true;
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
        output.add(NarratedElementType.USAGE,
            Component.literal(toggled ? "Recipe book is visible" : "Recipe book is hidden"));
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
