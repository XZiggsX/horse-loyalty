package com.example.horseloyalty.mixin;

import com.example.horseloyalty.config.HorseLoyaltyConfig;
import com.example.horseloyalty.loyalty.Family;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bred foals wander less: they skip some strolls, and never stroll to a spot far from their
 * parent (or, without a parent around, the player who bred them). Every other mob is untouched.
 */
@Mixin(RandomStrollGoal.class)
public abstract class RandomStrollGoalMixin {
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
		if (!cir.getReturnValueZ() || !(mob instanceof AbstractHorse foal) || !Family.isFamilyFoal(foal)
				|| !(foal.level() instanceof ServerLevel level)) {
			return;
		}
		Entity anchor = Family.parentToFollow(foal, level).map(Entity.class::cast)
				.or(() -> Family.breeder(foal, level))
				.orElse(null);
		if (anchor == null) {
			return;
		}
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		double maxDistance = config.foalStrollMaxDistance();
		if (foal.getRandom().nextDouble() < config.foalStrollSkipChance()
				|| anchor.distanceToSqr(wantedX, wantedY, wantedZ) > maxDistance * maxDistance) {
			cir.setReturnValue(false);
		}
	}
}
