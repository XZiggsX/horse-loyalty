package com.xziggsx.horseloyalty.riding.client.sense;

import com.xziggsx.horseloyalty.riding.api.Gait;

/**
 * The plain maths of the scan cone, with no game objects, so it can be tested on its own (see
 * {@code ConeMathTest}). Positions in the cone are relative to the horse: {@code forward} blocks
 * along its heading and {@code lateral} blocks to its right (negative is left).
 */
public final class ConeMath {
	/** The cone never reaches less far than this, in blocks. */
	public static final int MIN_DEPTH = 4;
	/** The cone never reaches further than this, in blocks. */
	public static final int MAX_DEPTH = 14;
	/** Blocks either side of the heading in the cone's first row. */
	public static final double NEAR_HALF_WIDTH = 1;
	/** Beyond this many blocks only every other row is sampled. */
	public static final int SPARSE_FROM = 8;
	/** Ticks between scans at a walk, where little changes between ticks. */
	public static final int SCAN_TICKS_SLOW = 4;
	/** Ticks between scans at a trot or faster. */
	public static final int SCAN_TICKS = 2;
	/** Fewer path columns than this in the cone means the road ends. */
	public static final int MIN_PATH_SAMPLES = 3;

	private ConeMath() {
	}

	/** Cone depth for this speed and look-ahead time, in whole blocks within the limits. */
	public static int depth(double blocksPerTick, int lookAheadTicks) {
		return Math.clamp(Math.round(blocksPerTick * lookAheadTicks), MIN_DEPTH, MAX_DEPTH);
	}

	/** Ticks until the next scan at this gait. */
	public static int scanTicks(Gait gait) {
		return gait.atLeast(Gait.TROT) ? SCAN_TICKS : SCAN_TICKS_SLOW;
	}

	/** Blocks either side of the heading in a row: widens evenly from the first row to the last. */
	public static double halfWidth(int row, int depth, double farHalfWidth) {
		if (depth <= 1) {
			return farHalfWidth;
		}
		double along = (row - 1) / (double) (depth - 1);
		return NEAR_HALF_WIDTH + (Math.max(farHalfWidth, NEAR_HALF_WIDTH) - NEAR_HALF_WIDTH) * along;
	}

	/** Whether a row is sampled: every row up close, every other one beyond {@link #SPARSE_FROM}. */
	public static boolean sampled(int row) {
		return row <= SPARSE_FROM || (row - SPARSE_FROM) % 2 == 0;
	}

	/** Which third of its row a column is in: -1 left, 0 middle, 1 right. */
	public static int lane(int lateral, double halfWidth) {
		return Math.abs(lateral) <= halfWidth / 3 ? 0 : Integer.signum(lateral);
	}

	/**
	 * How much a column counts: the squared cosine of its angle off the heading, so columns straight
	 * ahead count fully and ones off to the side less. Makes the straight-on branch win at a fork.
	 */
	public static double weight(double lateral, double forward) {
		double squared = lateral * lateral + forward * forward;
		return squared == 0 ? 1 : forward * forward / squared;
	}

	/** Angle from the heading to a point in the cone, in degrees: positive to the right. */
	public static float errorDegrees(double lateral, double forward) {
		return (float) Math.toDegrees(Math.atan2(lateral, forward));
	}

	/**
	 * The turn rate that curves the horse smoothly onto a point ahead, in degrees per tick (positive
	 * turns right, raising the yaw). This is pure pursuit: the circle through the horse and the point
	 * that starts along its heading. Following it at {@code blocksPerTick} takes
	 * {@code blocksPerTick * 2 sin(angle) / distance} radians a tick, so a far point gives an early,
	 * gentle turn and a near one a firmer turn. Times {@code gain}, capped at {@code maxTurn}.
	 * <p>
	 * Inside the dead zone the angle counts as 0; outside it the dead zone is taken off the angle, so
	 * the turn grows from 0 instead of jumping in.
	 */
	public static float pursuitTurn(double lateral, double forward, double blocksPerTick, double gain, float maxTurn,
			double deadZoneDegrees) {
		double distance = Math.hypot(lateral, forward);
		double angle = Math.atan2(lateral, forward);
		double deadZone = Math.toRadians(deadZoneDegrees);
		if (distance == 0 || Math.abs(angle) <= deadZone) {
			return 0;
		}
		angle -= Math.signum(angle) * deadZone;
		double radiansPerTick = blocksPerTick * 2 * Math.sin(angle) / distance;
		return (float) Math.clamp(Math.toDegrees(radiansPerTick) * gain, -maxTurn, maxTurn);
	}

	/** Moves {@code current} the given share (0 to 1) of the way to {@code target}: a smoothing step, once per tick. */
	public static float smooth(float current, float target, double share) {
		return (float) (current + (target - current) * share);
	}
}
