package com.xziggsx.horseloyalty.ai;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.loyalty.Family;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * A bred foal stays close to the parent it picked at birth (see {@link Family}), instead of
 * vanilla's FollowParentGoal, which trails whichever adult of the same species is nearest.
 * Keeps up from further away than vanilla, and hurries when it falls behind.
 */
public class FollowChosenParentGoal extends Goal {
	/** Beyond this many blocks the foal hurries to catch up. */
	private static final double HURRY_DISTANCE = 12;
	/** Stops this close to the parent. */
	private static final double STOP_DISTANCE = 3;

	private final AbstractHorse foal;
	private AbstractHorse parent;
	private int timeToRecalcPath;

	public FollowChosenParentGoal(AbstractHorse foal) {
		this.foal = foal;
		setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!Family.isFamilyFoal(foal)) {
			return false;
		}
		AbstractHorse found = Family.parentToFollow(foal, getServerLevel(foal)).orElse(null);
		if (found == null) {
			return false;
		}
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		double distance = foal.distanceToSqr(found);
		if (distance < sqr(config.foalFollowParentDistance()) || distance > sqr(config.foalFollowParentMaxDistance())) {
			return false;
		}
		parent = found;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (!Family.isFamilyFoal(foal) || parent == null || !parent.isAlive()) {
			return false;
		}
		double distance = foal.distanceToSqr(parent);
		return distance >= sqr(STOP_DISTANCE) && distance <= sqr(HorseLoyaltyConfig.get().foalFollowParentMaxDistance());
	}

	@Override
	public void start() {
		timeToRecalcPath = 0;
	}

	@Override
	public void stop() {
		parent = null;
	}

	@Override
	public void tick() {
		if (--timeToRecalcPath <= 0) {
			timeToRecalcPath = adjustedTickDelay(10);
			double speed = foal.distanceToSqr(parent) > sqr(HURRY_DISTANCE) ? 1.25 : 1.0;
			foal.getNavigation().moveTo(parent, speed);
		}
	}

	private static double sqr(double value) {
		return value * value;
	}
}
