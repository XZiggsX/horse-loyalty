package com.xziggsx.horseloyalty.dev.client.mixin;

import com.xziggsx.horseloyalty.dev.client.LoyaltyDebugHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks the development-only loyalty panel into the HUD. See {@link LoyaltyDebugHud}. */
@Mixin(Hud.class)
public abstract class HudMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void horseloyalty$extractDebugHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Hud hud = (Hud) (Object) this;
		if (LoyaltyDebugHud.ENABLED && !hud.isHidden() && !hud.getDebugOverlay().showDebugScreen()) {
			LoyaltyDebugHud.extract(graphics);
		}
	}
}
