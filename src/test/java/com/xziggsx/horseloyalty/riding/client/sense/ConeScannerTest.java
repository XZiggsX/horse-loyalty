package com.xziggsx.horseloyalty.riding.client.sense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The cone and path following on made-up worlds. The horse stands at (0.5, 0.5) facing +Z (yaw 0),
 * so the column {@code forward} rows ahead and {@code lateral} blocks to its right is block
 * (-lateral, forward): its right-hand side is -X.
 */
class ConeScannerTest {
	private static final SenseCone CONE = new SenseCone(10, 3);
	private static final double JUNCTION = 0.35;

	/** A flat world with path on the given blocks, and optionally nothing loaded from row {@code unloadedFrom} on. */
	private static final class World implements TerrainProbe {
		final Set<Long> path = new HashSet<>();
		int unloadedFrom = Integer.MAX_VALUE;
		int reads;

		World path(int lateral, int forward) {
			path.add(key(-lateral, forward));
			return this;
		}

		World straight(int lateral, int from, int to) {
			for (int forward = from; forward <= to; forward++) {
				path(lateral, forward);
			}
			return this;
		}

		@Override
		public Column probe(int x, int z, int feetY) {
			reads++;
			if (z >= unloadedFrom) {
				return Column.UNLOADED;
			}
			return new Column(path.contains(key(x, z)) ? Terrain.PATH : Terrain.GROUND, -1);
		}

		private static long key(int x, int z) {
			return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
		}
	}

	private static ConeScan scan(World world) {
		return ConeScanner.scan(world, 0.5, 0.5, 64, 0, CONE);
	}

	private static PathFollow.Decision decide(World world) {
		return PathFollow.decide(scan(world), JUNCTION);
	}

	@Test
	void coneIsATriangleWithSparseFarRows() {
		World world = new World();
		ConeScan scan = ConeScanner.scan(world, 0.5, 0.5, 64, 0, new SenseCone(14, 3));
		assertEquals(14, scan.depth());
		assertEquals(world.reads, scan.samples().size());
		assertEquals(3, scan.samples().stream().filter(sample -> sample.forward() == 1).count());
		assertEquals(7, scan.samples().stream().filter(sample -> sample.forward() == 14).count());
		assertTrue(scan.samples().stream().noneMatch(sample -> sample.forward() == 9));
	}

	@Test
	void eachBlockIsReadOnceAtAnAngle() {
		World world = new World();
		ConeScan scan = ConeScanner.scan(world, 0.5, 0.5, 64, 30, CONE);
		Set<String> blocks = new HashSet<>();
		double radians = Math.toRadians(30);
		for (ConeScan.Sample sample : scan.samples()) {
			int x = (int) Math.floor(0.5 - Math.sin(radians) * sample.forward() - Math.cos(radians) * sample.lateral());
			int z = (int) Math.floor(0.5 + Math.cos(radians) * sample.forward() - Math.sin(radians) * sample.lateral());
			assertTrue(blocks.add(x + "," + z), "block read twice: " + x + "," + z);
		}
	}

	@Test
	void straightPathIsAheadClear() {
		assertEquals(PathFollow.Outcome.AHEAD_CLEAR, decide(new World().straight(0, -2, 20)).outcome());
	}

	@Test
	void wideRoadIsAheadClear() {
		World world = new World();
		for (int lateral = -2; lateral <= 2; lateral++) {
			world.straight(lateral, -2, 20);
		}
		assertEquals(PathFollow.Outcome.AHEAD_CLEAR, decide(world).outcome());
	}

	@Test
	void crossroadsGoesStraightOn() {
		World world = new World().straight(0, -2, 20);
		for (int lateral = -6; lateral <= 6; lateral++) {
			world.path(lateral, 6);
		}
		assertEquals(PathFollow.Outcome.AHEAD_CLEAR, decide(world).outcome());
	}

	@Test
	void bendsAreFollowedTheRightWay() {
		World right = new World().straight(0, -2, 5);
		World left = new World().straight(0, -2, 5);
		for (int k = 1; k <= 10; k++) {
			right.path(k, 5);
			left.path(-k, 5);
		}
		PathFollow.Decision toRight = decide(right);
		PathFollow.Decision toLeft = decide(left);
		assertEquals(PathFollow.Outcome.FOLLOW, toRight.outcome());
		assertTrue(toRight.error() > 0, "turns right: " + toRight.error());
		assertEquals(PathFollow.Outcome.FOLLOW, toLeft.outcome());
		assertEquals(-toRight.error(), toLeft.error(), 1e-4);
	}

	@Test
	void diagonalPathAimsBetweenItsSteps() {
		// A staircase heading 45 degrees to the right: the aim is to the right, but well short of 45.
		World world = new World();
		for (int k = 0; k <= 12; k++) {
			world.path(k, k).path(k, k + 1);
		}
		PathFollow.Decision decision = decide(world);
		assertEquals(PathFollow.Outcome.FOLLOW, decision.outcome());
		assertTrue(decision.error() > 5 && decision.error() < 45, "error " + decision.error());
	}

	@Test
	void aimsAtTheFarHalfOfTheBend() {
		// A road shifting 2 blocks right from row 6 on: the aim lies on the far, shifted part.
		World world = new World().straight(0, -2, 5).straight(2, 6, 20);
		PathFollow.Decision decision = decide(world);
		assertEquals(PathFollow.Outcome.FOLLOW, decision.outcome());
		assertEquals(2, decision.aim().lateral(), 1e-9);
		assertTrue(decision.aim().forward() >= 6);
	}

	@Test
	void roadEnds() {
		assertEquals(PathFollow.Outcome.ROAD_END, decide(new World().straight(0, -2, 1)).outcome());
	}

	@Test
	void forkWithoutWayAheadIsAJunction() {
		World world = new World().straight(0, -2, 4);
		for (int k = 1; k <= 10; k++) {
			world.path(k, 4 + k).path(-k, 4 + k);
		}
		assertEquals(PathFollow.Outcome.JUNCTION, decide(world).outcome());
	}

	@Test
	void oneBlockGapIsStillFollowed() {
		World world = new World().straight(0, -2, 3).straight(0, 5, 20);
		PathFollow.Decision decision = decide(world);
		assertEquals(PathFollow.Outcome.FOLLOW, decision.outcome());
		assertEquals(0, decision.error(), 1e-4);
	}

	@Test
	void unloadedAheadWaits() {
		World world = new World().straight(0, -2, 20);
		world.unloadedFrom = 6;
		assertEquals(PathFollow.Outcome.NOT_LOADED, decide(world).outcome());
	}

	@Test
	void scanQueries() {
		ConeScan scan = scan(new World().straight(0, -2, 20));
		assertTrue(scan.aheadAll(Terrain.PATH));
		assertFalse(scan.aheadAny(Terrain.UNLOADED));
		// Rows 1 to 10 straight ahead, less row 9: beyond ConeMath.SPARSE_FROM only every other row is read.
		assertEquals(9, scan.count(Terrain.PATH));
		assertEquals(0, scan.centre(Terrain.PATH).lateral(), 1e-9);
		assertEquals(1, scan.shares(Terrain.PATH, 1).middle(), 1e-9);
		assertEquals(null, scan.centre(Terrain.WALL));
	}
}
