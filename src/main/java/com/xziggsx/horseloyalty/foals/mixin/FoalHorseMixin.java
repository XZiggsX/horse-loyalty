package com.xziggsx.horseloyalty.foals.mixin;

import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.foals.FoalStrolling;
import com.xziggsx.horseloyalty.foals.FoalsFeature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla nudges bred foals toward whichever horse is nearest. A foal with a family and a parent
 * nearby follows that parent instead, so this skips the nudge (see {@link FoalStrolling}).
 */
@Mixin(AbstractHorse.class)
public abstract class FoalHorseMixin {
	@Inject(method = "followMommy", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$skipNearestHorse(ServerLevel level, CallbackInfo ci) {
		if (!Features.isOn(FoalsFeature.ID)) {
			return;
		}
		if (FoalStrolling.skipsNearestHorse((AbstractHorse) (Object) this, level)) {
			ci.cancel();
		}
	}
}
