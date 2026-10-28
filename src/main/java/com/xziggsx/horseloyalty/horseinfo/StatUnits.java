package com.xziggsx.horseloyalty.horseinfo;

/**
 * Horse attributes as numbers players understand, and where they sit in vanilla's range for a
 * wild horse (for the stat bars).
 */
public final class StatUnits {
	/** Movement speed attribute to blocks per second on flat ground (player walk speed is 4.317 b/s at 0.098). */
	private static final double SPEED_TO_BLOCKS_PER_SECOND = 4.317 / (0.1 * 0.98);

	public static final double MIN_SPEED = 0.1125;
	public static final double MAX_SPEED = 0.3375;
	public static final double MIN_JUMP = 0.4;
	public static final double MAX_JUMP = 1.0;

	private StatUnits() {
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
