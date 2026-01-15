package com.chayut.bottomlessinventory.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A toggle button widget for the recipe book.
 * Uses vanilla recipe book button sprites for consistent look.
 * Full recipe book integration will be implemented in a later phase.
 */
public class RecipeBookButtonWidget extends Button {
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 18;

    // Vanilla recipe book button sprites
    private static final ResourceLocation BUTTON_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/button_highlighted");

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
        // Choose sprite based on hover state
        ResourceLocation sprite = isHovered() ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE;

        // Render the vanilla sprite
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), BUTTON_WIDTH, BUTTON_HEIGHT);

        // Draw a subtle overlay when toggled to indicate active state
        if (toggled) {
            // Green tint overlay to show recipe book is open
            graphics.fill(getX(), getY(), getX() + BUTTON_WIDTH, getY() + BUTTON_HEIGHT, 0x4000FF00);
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
