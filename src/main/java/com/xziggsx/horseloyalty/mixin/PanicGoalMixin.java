package com.xziggsx.horseloyalty.mixin;

import com.xziggsx.horseloyalty.personality.Personality;
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
public abstract class PanicGoalMixin {
	@Shadow
	@Final
	protected PathfinderMob mob;

	@Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
	private void horseloyalty$bravery(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && mob instanceof AbstractHorse horse && Personality.of(horse) == Personality.BRAVE
				&& !horse.isOnFire() && !horse.isFreezing()) {
			cir.setReturnValue(false);
		}
	}
}
