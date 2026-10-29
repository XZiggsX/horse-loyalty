package com.xziggsx.horseloyalty.riding.client.sense;

import com.xziggsx.horseloyalty.riding.RidingSettings;

/**
 * The horse follows the path it's on while the rider doesn't steer: it curves smoothly toward the
 * weighted middle of the far half of the path ahead (see {@link ConeMath#pursuitTurn}), so it turns
 * into a bend early and gently rather than correcting at the last moment. It only steers, never changes the gait, and is the same for every horse
 * and rider. Where the way on isn't clear (the road ends, a fork, unloaded chunks) it lets go and
 * the horse keeps its heading. While it's in charge, gait drops come less often.
 */
public final class PathFollow implements RideBehaviour {
	/** Below this share of the far path weight, the middle counts as empty at a fork. */
	static final double EMPTY_MIDDLE = 0.1;

	/** What the horse makes of the path ahead. */
	public enum Outcome {
		/** Not following: off the path, the rider steering, or not allowed now. */
		OFF,
		/** The path runs straight on as far as the horse looks: no turn needed. */
		AHEAD_CLEAR,
		/** Turning toward the path. */
		FOLLOW,
		/** Too little path ahead: the horse keeps its heading. */
		ROAD_END,
		/** Path to the left and right but not ahead: the rider chooses. */
		JUNCTION,
		/** The way ahead isn't loaded yet. */
		NOT_LOADED
	}

	/**
	 * What the horse makes of a scan.
	 *
	 * @param outcome what it makes of it
	 * @param error   for {@link Outcome#FOLLOW}, the angle to the aim point in degrees (positive right)
	 * @param aim     for {@link Outcome#FOLLOW}, the point on the path the horse curves toward, else null
	 */
	public record Decision(Outcome outcome, float error, ConeScan.Point aim) {
		Decision(Outcome outcome) {
			this(outcome, 0, null);
		}
	}

	/** Ticks left before the horse follows again after the rider steered. */
	private int graceTicks;
	/** Ticks on the ground since the horse last stood on a path. */
	private int offPathTicks = Integer.MAX_VALUE;
	private boolean active;
	private Decision last = new Decision(Outcome.OFF);
	private float lastTarget = Float.NaN;
	/** The smoothed turn target, so a new scan's aim eases in instead of jumping. */
	private float smoothed;

	/**
	 * What a horse makes of a scan. A junction and the aim point use the far half of the cone, since
	 * the near half is the road the horse is already on; with no path there, the aim uses all of it.
	 *
	 * @param junctionShare above this share of the far path weight on both sides, with the middle empty, it's a junction
	 */
	public static Decision decide(ConeScan scan, double junctionShare) {
		if (scan.aheadAny(Terrain.UNLOADED)) {
			return new Decision(Outcome.NOT_LOADED);
		}
		if (scan.aheadAll(Terrain.PATH)) {
			return new Decision(Outcome.AHEAD_CLEAR);
		}
		if (scan.count(Terrain.PATH) < ConeMath.MIN_PATH_SAMPLES) {
			return new Decision(Outcome.ROAD_END);
		}
		int farFrom = scan.depth() / 2 + 1;
		ConeScan.Shares far = scan.shares(Terrain.PATH, farFrom);
		if (far.left() > junctionShare && far.right() > junctionShare && far.middle() < EMPTY_MIDDLE) {
			return new Decision(Outcome.JUNCTION);
		}
		ConeScan.Point aim = scan.centre(Terrain.PATH, farFrom);
		if (aim == null) {
			aim = scan.centre(Terrain.PATH);
		}
		return new Decision(Outcome.FOLLOW, ConeMath.errorDegrees(aim.lateral(), aim.forward()), aim);
	}

	@Override
	public boolean wantsScan(SenseState state) {
		RidingSettings settings = RidingSettings.get();
		return settings.pathFollowingEnabled() && state.allowed() && !state.steering() && graceTicks == 0
				&& offPathTicks <= settings.pathOffPathTicks();
	}

	@Override
	public float tick(SenseState state, ConeScan scan) {
		RidingSettings settings = RidingSettings.get();
		if (state.steering()) {
			graceTicks = settings.pathGraceTicks();
		} else if (graceTicks > 0) {
			graceTicks--;
		}
		// In the air the horse can't be on or off anything, so jumping along a path doesn't count as leaving it.
		if (state.underfoot() == Terrain.PATH) {
			offPathTicks = 0;
		} else if (state.underfoot() != null && offPathTicks < Integer.MAX_VALUE) {
			offPathTicks++;
		}
		active = settings.pathFollowingEnabled() && state.allowed() && !state.steering() && graceTicks == 0
				&& offPathTicks <= settings.pathOffPathTicks();
		if (!active || scan == null) {
			last = new Decision(Outcome.OFF);
			lastTarget = Float.NaN;
			smoothed = 0;
			return lastTarget;
		}
		last = decide(scan, settings.pathJunctionShare());
		float target = switch (last.outcome()) {
			case AHEAD_CLEAR -> 0;
			case FOLLOW -> ConeMath.pursuitTurn(last.aim().lateral(), last.aim().forward(), state.blocksPerTick(),
					settings.pathGain(), settings.pathTurnDegreesPerTick(state.gait()), settings.pathDeadZoneDegrees());
			default -> Float.NaN;
		};
		if (Float.isNaN(target)) {
			// Letting go: the horse keeps its heading, and the ride's own easing straightens it out.
			smoothed = 0;
			lastTarget = Float.NaN;
		} else {
			smoothed = ConeMath.smooth(smoothed, target, settings.pathSmoothingShare());
			lastTarget = smoothed;
		}
		return lastTarget;
	}

	@Override
	public boolean active() {
		return active;
	}

	@Override
	public void reset() {
		graceTicks = 0;
		offPathTicks = Integer.MAX_VALUE;
		active = false;
		last = new Decision(Outcome.OFF);
		lastTarget = Float.NaN;
		smoothed = 0;
	}

	/** The latest decision, for the dev HUD. */
	public Decision lastDecision() {
		return last;
	}

	/** The latest turn target in degrees per tick, NaN if none. For the dev HUD. */
	public float lastTarget() {
		return lastTarget;
	}
}
