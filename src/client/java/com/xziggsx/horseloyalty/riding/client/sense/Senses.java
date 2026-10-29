package com.xziggsx.horseloyalty.riding.client.sense;

import com.xziggsx.horseloyalty.riding.RidingSettings;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.client.RideNumbers;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;

/**
 * What a ridden horse senses ahead of itself, and what it does about it. Scans the cone once (only
 * when a behaviour wants it) and asks each {@link RideBehaviour} in turn for a turn target; the
 * first one with a target steers. Path following is the only behaviour so far; braking and swerving
 * are meant to join it, reading the same scan.
 * <p>
 * One per horse, owned by its {@code RideControl}. Client side only: riding runs on the rider's client.
 */
public final class Senses {
	private final PathFollow path = new PathFollow();
	/** Every behaviour, highest priority first. */
	private final List<RideBehaviour> behaviours = List.of(path);

	private ColumnCache probe;
	private Level probeLevel;
	private ConeScan scan;
	private int scanTimer;
	private float turnTarget = Float.NaN;
	private long scanNanos;

	/**
	 * Runs once per riding tick.
	 *
	 * @param allowed  whether a behaviour may steer now (not halted, backing up, sidestepping or bolting)
	 * @param steering whether the rider is holding A or D
	 */
	public void tick(AbstractHorse horse, Gait gait, boolean allowed, boolean steering) {
		RidingSettings settings = RidingSettings.get();
		if (!settings.pathFollowingEnabled()) {
			if (scan != null || path.active()) {
				reset();
			}
			return;
		}
		ColumnCache probe = probeFor(horse.level());
		probe.setTime(horse.level().getGameTime());
		// A dirt path is 15/16 of a block tall; this puts the feet in the block above it, as on full blocks.
		int feetY = Mth.floor(horse.getY() + 0.1);
		Terrain underfoot = horse.onGround()
				? probe.probe(Mth.floor(horse.getX()), Mth.floor(horse.getZ()), feetY).terrain() : null;
		double blocksPerTick = RideNumbers.blocksPerTick(gait, horse.getAttributeValue(Attributes.MOVEMENT_SPEED));
		SenseState state = new SenseState(gait, allowed, steering, underfoot, blocksPerTick);

		boolean wanted = false;
		for (RideBehaviour behaviour : behaviours) {
			wanted |= behaviour.wantsScan(state);
		}
		if (!wanted) {
			scan = null;
			scanTimer = 0;
		} else if (horse.onGround() && --scanTimer <= 0) {
			scanTimer = ConeMath.scanTicks(gait);
			SenseCone cone = SenseCone.of(blocksPerTick, settings.pathLookAheadTicks(), settings.pathWidthBlocks(gait));
			long start = System.nanoTime();
			scan = ConeScanner.scan(probe, horse.getX(), horse.getZ(), feetY, horse.getYRot(), cone);
			scanNanos = System.nanoTime() - start;
		}

		turnTarget = Float.NaN;
		for (RideBehaviour behaviour : behaviours) {
			float target = behaviour.tick(state, scan);
			if (Float.isNaN(turnTarget)) {
				turnTarget = target;
			}
		}
	}

	/** The turn rate a behaviour aims for this tick, in degrees per tick (positive turns right), or NaN for none. */
	public float turnTarget() {
		return turnTarget;
	}

	/** Path following. */
	public PathFollow path() {
		return path;
	}

	/** The latest scan, or null. For the dev HUD. */
	public ConeScan lastScan() {
		return scan;
	}

	/** How long the latest scan took, in nanoseconds. For the dev HUD. */
	public long scanNanos() {
		return scanNanos;
	}

	/** Starts over, at the start of a ride. */
	public void reset() {
		for (RideBehaviour behaviour : behaviours) {
			behaviour.reset();
		}
		scan = null;
		scanTimer = 0;
		turnTarget = Float.NaN;
		if (probe != null) {
			probe.clear();
		}
	}

	/** The cached probe for this level, made anew when the horse changed level. */
	private ColumnCache probeFor(Level level) {
		if (probe == null || probeLevel != level) {
			probe = new ColumnCache(new LevelTerrainProbe(level));
			probeLevel = level;
		}
		return probe;
	}
}
