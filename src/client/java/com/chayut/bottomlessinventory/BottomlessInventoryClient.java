package com.chayut.bottomlessinventory;

import com.chayut.bottomlessinventory.client.ClientInventoryCache;
import net.fabricmc.api.ClientModInitializer;

public class BottomlessInventoryClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register client-side packet receivers
		ClientInventoryCache.register();
	}
}