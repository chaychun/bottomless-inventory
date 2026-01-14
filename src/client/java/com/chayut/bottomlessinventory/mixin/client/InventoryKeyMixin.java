package com.chayut.bottomlessinventory.mixin.client;

import com.chayut.bottomlessinventory.network.packets.OpenInventoryPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept the inventory key press and open our bottomless inventory
 * instead of the vanilla inventory screen.
 */
@Mixin(Minecraft.class)
public abstract class InventoryKeyMixin {

    @Shadow
    public abstract void setScreen(net.minecraft.client.gui.screens.Screen screen);

    /**
     * Intercepts the handleKeybinds method to catch inventory key presses.
     *
     * In Minecraft 1.21.x, this method is called every tick and checks for key presses.
     * We inject at HEAD with cancellable=true so we can prevent the vanilla behavior.
     */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void onHandleKeybinds(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;

        // Check if the inventory key was pressed
        while (minecraft.options.keyInventory.consumeClick()) {
            // Send packet to server to open our bottomless inventory
            if (ClientPlayNetworking.canSend(OpenInventoryPacket.TYPE)) {
                ClientPlayNetworking.send(new OpenInventoryPacket());
            }

            // The vanilla code that would normally run is:
            // if (this.player.isSpectator()) {
            //     this.gui.getSpectatorGui().onHotbarSelected(this.player.getInventory().selected);
            // } else {
            //     this.getTutorial().onOpenInventory();
            //     this.player.openMenu(new InventoryMenuProvider());
            // }
            //
            // By consuming the click here and sending our packet, we prevent the vanilla
            // inventory from opening. Our server-side handler will open our screen instead.
        }
    }
}
