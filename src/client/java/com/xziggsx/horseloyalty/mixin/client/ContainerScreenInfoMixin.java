package com.xziggsx.horseloyalty.mixin.client;

import com.xziggsx.horseloyalty.client.horseinfo.HorseScreenAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While the horse info view is showing, the slots are still in the menu but must not be seen or
 * used. With no slot ever under the mouse, clicks, shift-clicks, drags, double-clicks, number
 * keys, the offhand key and dropping all have nothing to act on; clicks outside the window don't
 * count either. Only acts on a horse screen with its info view open.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenInfoMixin {
	@Unique
	private boolean horseloyalty$infoOpen() {
		return this instanceof HorseScreenAccess screen && screen.horseloyalty$infoOpen();
	}

	@Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$noHoveredSlot(double mouseX, double mouseY, CallbackInfoReturnable<Slot> cir) {
		if (horseloyalty$infoOpen()) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$noClickOutside(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> cir) {
		if (horseloyalty$infoOpen()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "extractSlots", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$noSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (horseloyalty$infoOpen()) {
			ci.cancel();
		}
	}

	/** The info view draws its own title, and has no "Inventory" label. */
	@Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$noLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (horseloyalty$infoOpen()) {
			ci.cancel();
		}
	}
}
