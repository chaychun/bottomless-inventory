package com.chayut.bottomlessinventory;

import com.chayut.bottomlessinventory.client.ClientInventoryCache;
import com.chayut.bottomlessinventory.client.screen.BottomlessInventoryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class BottomlessInventoryClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register client-side packet receivers
		ClientInventoryCache.register();

		// Register the screen for the bottomless inventory handler
		MenuScreens.register(BottomlessInventory.BOTTOMLESS_SCREEN_HANDLER_TYPE, BottomlessInventoryScreen::new);
	}
}