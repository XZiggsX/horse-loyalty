package com.xziggsx.horseloyalty.personality.mixin;

import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.personality.Personalities;
import com.xziggsx.horseloyalty.personality.PersonalityFeature;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Brave horses stand their ground when hit: they only panic when burning or freezing.
 * Horses use MountPanicGoal, a PanicGoal subclass, so this hooks PanicGoal and checks for horses.
 */
@Mixin(PanicGoal.class)
public abstract class BravePanicMixin {
	@Shadow
	@Final
	protected PathfinderMob mob;

	@Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$bravery(CallbackInfoReturnable<Boolean> cir) {
		if (!Features.isOn(PersonalityFeature.ID)) {
			return;
		}
		if (cir.getReturnValueZ() && mob instanceof AbstractHorse horse && Personalities.standsGround(horse)) {
			cir.setReturnValue(false);
		}
	}
}
