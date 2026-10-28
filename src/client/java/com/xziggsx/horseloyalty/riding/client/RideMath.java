package com.xziggsx.horseloyalty.riding.client;

/**
 * The plain maths of riding, with no game objects, so it can be tested on its own (see
 * {@code RideMathTest}). {@link RideControl} feeds it the rider's keys and the horse's numbers.
 */
public final class RideMath {
	private RideMath() {
	}

	/**
	 * The turn rate this tick: moves {@code current} toward {@code target} by at most {@code ease},
	 * so a turn eases in and out instead of snapping.
	 */
	public static float easeTurn(float current, float target, float ease) {
		return current + Math.clamp(target - current, -ease, ease);
	}

	/**
	 * The turn rate the horse aims for, in degrees per tick. A bolting horse veers on its own
	 * ({@code boltTurn} -1 left, 0 straight, 1 right); a sidestepping one doesn't turn; otherwise A
	 * and D turn it. {@code sideKey} is the rider's sideways input: positive for A (left), which
	 * lowers the yaw.
	 */
	public static float turnTarget(boolean bolting, int boltTurn, boolean sidestepping, float sideKey, float maxRate) {
		if (bolting) {
			return boltTurn * maxRate;
		}
		return sidestepping ? 0 : Math.signum(sideKey) * -maxRate;
	}

	/** The forward input for the horse: -1 backing up, 0 at a halt, 1 at any other gait. */
	public static double forwardInput(boolean reversing, boolean halted) {
		return reversing ? -1 : halted ? 0 : 1;
	}

	/**
	 * The sideways input while sidestepping. Moving, the angle off straight ahead sets the
	 * sideways share; halted, it's a pure sidestep. A positive x input moves left, like A.
	 */
	public static double sideInput(double forwardInput, float sideKey, double angleDegrees) {
		double side = forwardInput == 0 ? 1 : Math.tan(Math.toRadians(angleDegrees));
		return Math.signum(sideKey) * side;
	}

	/** How hard a gallop is pushed, from 0 to 1, given the push and the most it can be. */
	public static double effort(double push, double max) {
		return max > 0 ? Math.min(1, push / max) : 0;
	}

	/** The push after one tick of fading: shrinks by the same share every tick. */
	public static double fade(double push, double fadePerTick) {
		return push * fadePerTick;
	}
}
