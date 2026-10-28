package com.xziggsx.horseloyalty.foals;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Bred foals wander less: they skip some strolls, and never stroll to a spot far from their
 * parent (or, without a parent around, the player who bred them). Vanilla also nudges bred foals
 * toward whichever horse is nearest; a foal with a parent nearby follows that parent instead.
 */
public final class FoalStrolling {
	private FoalStrolling() {
	}

	/** Whether a foal turns down a random stroll to this spot. False for anything that isn't a family foal. */
	public static boolean refuses(AbstractHorse foal, double x, double y, double z) {
		if (!Family.isFamilyFoal(foal) || !(foal.level() instanceof ServerLevel level)) {
			return false;
		}
		Entity anchor = Family.parentToFollow(foal, level).map(Entity.class::cast)
				.or(() -> Family.breeder(foal, level))
				.orElse(null);
		if (anchor == null) {
			return false;
		}
		FoalSettings config = FoalSettings.get();
		double maxDistance = config.strollMaxDistance();
		return foal.getRandom().nextDouble() < config.strollSkipChance()
				|| anchor.distanceToSqr(x, y, z) > maxDistance * maxDistance;
	}

	/** Whether to skip vanilla's nudge toward the nearest horse: the foal has a family and a parent nearby. */
	public static boolean skipsNearestHorse(AbstractHorse foal, ServerLevel level) {
		return Family.isFamilyFoal(foal) && Family.parentToFollow(foal, level).isPresent();
	}
}
