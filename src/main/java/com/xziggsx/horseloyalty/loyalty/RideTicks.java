package com.xziggsx.horseloyalty.loyalty;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Counts riding time for whoever is in the saddle while the horse is moving. A rider's client
 * moves the horse between server ticks, so "moving" compares the horse's position with where it
 * was last tick (the entity's own previous position is reset at the start of the tick and can't
 * be used for this).
 */
final class RideTicks {
	/** Each horse's position last tick, x and z. Server thread only; forgotten with the horse. */
	private static final Map<AbstractHorse, double[]> LAST_POSITION = new WeakHashMap<>();

	private RideTicks() {
	}

	/** Every server tick: one ride tick for the rider of a tamed horse that moved since last tick. */
	static void tick(AbstractHorse horse, ServerLevel level) {
		double[] last = LAST_POSITION.computeIfAbsent(horse, unused -> new double[] {Double.NaN, Double.NaN});
		double dx = horse.getX() - last[0];
		double dz = horse.getZ() - last[1];
		last[0] = horse.getX();
		last[1] = horse.getZ();
		if (horse.isTamed() && horse.getControllingPassenger() instanceof Player player
				&& dx * dx + dz * dz >= Loyalty.MIN_RIDE_MOVE_SQR) {
			Loyalty.recordRideTick(horse, player);
		}
	}
}
