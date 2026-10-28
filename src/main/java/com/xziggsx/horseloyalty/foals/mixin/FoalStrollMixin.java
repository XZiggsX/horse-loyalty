package com.xziggsx.horseloyalty.foals.mixin;

import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.foals.FoalStrolling;
import com.xziggsx.horseloyalty.foals.FoalsFeature;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bred foals wander less, see {@link FoalStrolling}. Every other mob is untouched. */
@Mixin(RandomStrollGoal.class)
public abstract class FoalStrollMixin {
	@Shadow
	@Final
	protected PathfinderMob mob;

	@Shadow
	protected double wantedX;

	@Shadow
	protected double wantedY;

	@Shadow
	protected double wantedZ;

	@Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$stayNearFamily(CallbackInfoReturnable<Boolean> cir) {
		if (!Features.isOn(FoalsFeature.ID)) {
			return;
		}
		if (cir.getReturnValueZ() && mob instanceof AbstractHorse foal && FoalStrolling.refuses(foal, wantedX, wantedY, wantedZ)) {
			cir.setReturnValue(false);
		}
	}
}
