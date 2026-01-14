package com.chayut.bottomlessinventory;

import com.chayut.bottomlessinventory.data.ModAttachments;
import com.chayut.bottomlessinventory.network.BottomlessNetworking;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BottomlessInventory implements ModInitializer {
	public static final String MOD_ID = "bottomless-inventory";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Bottomless Inventory mod");

		// Register attachments
		ModAttachments.register();

		// Register networking
		BottomlessNetworking.register();

		LOGGER.info("Bottomless Inventory mod initialized successfully");
	}
}