package com.chayut.bottomlessinventory;

import com.chayut.bottomlessinventory.data.ModAttachments;
import com.chayut.bottomlessinventory.network.BottomlessNetworking;
import com.chayut.bottomlessinventory.network.InventorySyncHandler;
import com.chayut.bottomlessinventory.network.OpenInventoryHandler;
import com.chayut.bottomlessinventory.screen.BottomlessScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BottomlessInventory implements ModInitializer {
	public static final String MOD_ID = "bottomless-inventory";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Screen handler type for the bottomless inventory screen
	public static final MenuType<BottomlessScreenHandler> BOTTOMLESS_SCREEN_HANDLER_TYPE =
			new MenuType<>(BottomlessScreenHandler::new, FeatureFlags.DEFAULT_FLAGS);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Bottomless Inventory mod");

		// Register screen handler type
		Registry.register(
			BuiltInRegistries.MENU,
			ResourceLocation.fromNamespaceAndPath(MOD_ID, "bottomless_inventory"),
			BOTTOMLESS_SCREEN_HANDLER_TYPE
		);

		// Register attachments
		ModAttachments.register();

		// Register networking
		BottomlessNetworking.register();

		// Register sync handlers (must be after networking)
		InventorySyncHandler.register();

		// Register open inventory handler (must be after networking)
		OpenInventoryHandler.register();

		LOGGER.info("Bottomless Inventory mod initialized successfully");
	}
}