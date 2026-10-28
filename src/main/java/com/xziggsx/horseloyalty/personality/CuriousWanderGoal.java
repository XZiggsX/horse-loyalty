package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.personality.api.Personality;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Now and then a curious horse wanders off toward the nearest village meeting point (its bell),
 * if there's one within the configured radius. Only grown horses that are loose: not ridden,
 * not on a lead. Fences and leads keep a curious horse home.
 */
public class CuriousWanderGoal extends Goal {
	/** Stops this close to the bell; the village itself is the destination, not the bell. */
	private static final double ARRIVED_DISTANCE = 10;
	/** Gives up after a minute, even if it hasn't arrived. */
	private static final int MAX_WANDER_TICKS = 1200;

	private final AbstractHorse horse;
	private BlockPos target;
	private int ticksLeft;

	public CuriousWanderGoal(AbstractHorse horse) {
		this.horse = horse;
		setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (Personalities.of(horse) != Personality.CURIOUS || horse.isBaby() || horse.isVehicle() || horse.isLeashed()) {
			return false;
		}
		PersonalitySettings config = PersonalitySettings.get();
		// Cheap random check first: the village search below is the expensive part.
		if (horse.getRandom().nextInt(reducedTickDelay(config.curiousWanderEveryTicks())) != 0) {
			return false;
		}
		ServerLevel level = getServerLevel(horse);
		BlockPos found = level.getPoiManager()
				.findClosest(type -> type.is(PoiTypes.MEETING), horse.blockPosition(), config.curiousSearchRadius(),
						PoiManager.Occupancy.ANY)
				.orElse(null);
		if (found == null || found.closerToCenterThan(horse.position(), ARRIVED_DISTANCE)) {
			return false;
		}
		target = found;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return target != null && ticksLeft > 0 && !horse.getNavigation().isDone() && !horse.isVehicle()
				&& !horse.isLeashed() && !target.closerToCenterThan(horse.position(), ARRIVED_DISTANCE);
	}

	@Override
	public void start() {
		ticksLeft = MAX_WANDER_TICKS;
		horse.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
	}

	@Override
	public void tick() {
		ticksLeft--;
	}

	@Override
	public void stop() {
		target = null;
		horse.getNavigation().stop();
	}
}
