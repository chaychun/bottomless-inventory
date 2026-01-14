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
        // Determine which texture to use based on toggle state
        int u = toggled ? ACTIVE_U : INACTIVE_U;
        int v = toggled ? ACTIVE_V : INACTIVE_V;

        // Draw the button texture
        graphics.blit(
            RECIPE_BUTTON_LOCATION,
            getX(),
            getY(),
            u,
            v,
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT
        );

        // Draw hover overlay if mouse is over the button
        if (isHovered()) {
            graphics.fill(
                getX(),
                getY(),
                getX() + BUTTON_WIDTH,
                getY() + BUTTON_HEIGHT,
                0x40FFFFFF // Semi-transparent white overlay
            );
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
