package com.xziggsx.horseloyalty.foals;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * A bred foal trots after the player who bred it, but only while its parent isn't close by:
 * the parent always comes first (see {@link FollowChosenParentGoal}).
 */
public class FollowBreederGoal extends Goal {
	/** The foal sticks with its parent while the parent is this close. */
	private static final double PARENT_NEAR_DISTANCE = 8;
	/** Stops this close to the player. */
	private static final double STOP_DISTANCE = 2;

	private final AbstractHorse foal;
	private ServerPlayer breeder;
	private int timeToRecalcPath;

	public FollowBreederGoal(AbstractHorse foal) {
		this.foal = foal;
		setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (!Family.isFamilyFoal(foal)) {
			return false;
		}
		ServerLevel level = getServerLevel(foal);
		ServerPlayer found = Family.breeder(foal, level).orElse(null);
		if (found == null || parentIsNear(level)) {
			return false;
		}
		FoalSettings config = FoalSettings.get();
		double distance = foal.distanceToSqr(found);
		if (distance < sqr(config.followPlayerDistance()) || distance > sqr(config.followPlayerMaxDistance())) {
			return false;
		}
		breeder = found;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (!Family.isFamilyFoal(foal) || breeder == null || !breeder.isAlive() || breeder.isSpectator()
				|| breeder.level() != foal.level() || parentIsNear(getServerLevel(foal))) {
			return false;
		}
		double distance = foal.distanceToSqr(breeder);
		return distance >= sqr(STOP_DISTANCE) && distance <= sqr(FoalSettings.get().followPlayerMaxDistance());
	}

	@Override
	public void start() {
		timeToRecalcPath = 0;
	}

	@Override
	public void stop() {
		breeder = null;
	}

	@Override
	public void tick() {
		if (--timeToRecalcPath <= 0) {
			timeToRecalcPath = adjustedTickDelay(10);
			foal.getNavigation().moveTo(breeder, 1.1);
		}
	}

	private boolean parentIsNear(ServerLevel level) {
		return Family.parentToFollow(foal, level)
				.map(parent -> foal.distanceToSqr(parent) <= sqr(PARENT_NEAR_DISTANCE))
				.orElse(false);
	}

	private static double sqr(double value) {
		return value * value;
	}
}
