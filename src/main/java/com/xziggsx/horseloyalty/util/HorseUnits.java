package com.xziggsx.horseloyalty.util;

/**
 * Horse numbers in units players understand, and vanilla's ranges for them. The one place for
 * these conversions: riding, the dev panel and the horse info screen all use it, on both sides.
 */
public final class HorseUnits {
	/** Unenchanted player walking pace as an effective speed (0.1 speed, 0.98 input), about 4.3 blocks/s. */
	public static final double PLAYER_WALK_SPEED = 0.1 * 0.98;
	/** Converts an effective speed or movement speed attribute to blocks per second on flat ground. */
	public static final double SPEED_TO_BLOCKS_PER_SECOND = 4.317 / PLAYER_WALK_SPEED;

	/** Vanilla's range for a horse's movement speed attribute. */
	public static final double MIN_SPEED = 0.1125;
	public static final double MAX_SPEED = 0.3375;
	/** Vanilla's range for a horse's jump strength attribute. */
	public static final double MIN_JUMP = 0.4;
	public static final double MAX_JUMP = 1.0;

	private HorseUnits() {
	}

	/** Top speed in blocks per second for a movement speed attribute. */
	public static double blocksPerSecond(double speedAttribute) {
		return speedAttribute * SPEED_TO_BLOCKS_PER_SECOND;
	}

	/** About how many blocks high a horse with this jump strength clears (the community's fitted curve). */
	public static double jumpBlocks(double jumpStrength) {
		double x = jumpStrength;
		return Math.max(0, -0.1817584952 * x * x * x + 3.689713992 * x * x + 2.128599134 * x - 0.343930367);
	}

	/** Where a value sits between low and high, from 0 to 1. */
	public static double fill(double value, double low, double high) {
		return high <= low ? 1 : Math.clamp((value - low) / (high - low), 0, 1);
	}
}
