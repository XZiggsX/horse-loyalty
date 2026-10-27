package com.xziggsx.horseloyalty.ai;

import com.xziggsx.horseloyalty.horn.HornCall;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * A horse called with a horn (see {@link HornCall}) runs to the player who called it, then stops
 * and whinnies. It gives up if it can't get there in time, if someone mounts or leashes it, or if
 * the player leaves.
 */
public class ComeWhenCalledGoal extends Goal {
	/** Stops this close to the player. */
	private static final double STOP_DISTANCE = 3;
	/** Navigation speed: well above vanilla's panic speed (1.2), so it looks like a run. */
	private static final double SPEED = 2.2;

	private final AbstractHorse horse;
	private Player caller;
	private int timeToRecalcPath;

	public ComeWhenCalledGoal(AbstractHorse horse) {
		this.horse = horse;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		Player found = activeCaller();
		if (found == null) {
			return false;
		}
		if (isClose(found)) {
			arrive(found);
			return false;
		}
		caller = found;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return caller != null && activeCaller() == caller && !isClose(caller);
	}

	@Override
	public void start() {
		timeToRecalcPath = 0;
	}

	@Override
	public void stop() {
		if (caller != null && isClose(caller)) {
			arrive(caller);
		}
		caller = null;
		horse.getNavigation().stop();
	}

	@Override
	public void tick() {
		horse.getLookControl().setLookAt(caller, 10.0F, horse.getMaxHeadXRot());
		if (--timeToRecalcPath <= 0) {
			timeToRecalcPath = adjustedTickDelay(10);
			horse.getNavigation().moveTo(caller, SPEED);
		}
	}

	/** The player this horse is answering, or null (and the call is dropped) if it shouldn't any more. */
	private Player activeCaller() {
		HornCall.Call call = horse.getAttached(HornCall.CALL);
		if (call == null) {
			return null;
		}
		Player player = horse.level().getPlayerByUUID(call.caller());
		if (horse.level().getGameTime() > call.until() || horse.isVehicle() || horse.isLeashed() || horse.isPassenger()
				|| player == null || !player.isAlive() || player.isSpectator()) {
			horse.removeAttached(HornCall.CALL);
			return null;
		}
		return player;
	}

	/** Within {@link #STOP_DISTANCE} of the player. */
	private boolean isClose(Player player) {
		return horse.distanceToSqr(player) <= STOP_DISTANCE * STOP_DISTANCE;
	}

	/** Made it: the call is answered, and the horse greets the player. */
	private void arrive(Player player) {
		horse.removeAttached(HornCall.CALL);
		horse.getLookControl().setLookAt(player);
		horse.playAmbientSound();
	}
}
