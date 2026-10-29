package com.xziggsx.horseloyalty.riding.client.sense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConeMathTest {
	@Test
	void depthFollowsSpeedWithinLimits() {
		// A second (20 ticks) of travel: a walk of 0.2 blocks/tick is under the minimum, a fast gallop over the maximum.
		assertEquals(ConeMath.MIN_DEPTH, ConeMath.depth(0.15, 20));
		assertEquals(10, ConeMath.depth(0.5, 20));
		assertEquals(ConeMath.MAX_DEPTH, ConeMath.depth(1.0, 20));
	}

	@Test
	void coneWidensEvenly() {
		assertEquals(1, ConeMath.halfWidth(1, 10, 3), 1e-9);
		assertEquals(3, ConeMath.halfWidth(10, 10, 3), 1e-9);
		// A one-row cone is all far end.
		assertEquals(2, ConeMath.halfWidth(1, 1, 2), 1e-9);
		// Never narrower than the first row, even if set so.
		assertEquals(1, ConeMath.halfWidth(10, 10, 0.5), 1e-9);
	}

	@Test
	void farRowsAreSparse() {
		assertTrue(ConeMath.sampled(1));
		assertTrue(ConeMath.sampled(ConeMath.SPARSE_FROM));
		assertFalse(ConeMath.sampled(ConeMath.SPARSE_FROM + 1));
		assertTrue(ConeMath.sampled(ConeMath.SPARSE_FROM + 2));
	}

	@Test
	void lanesSplitRowsInThirds() {
		assertEquals(0, ConeMath.lane(0, 1));
		assertEquals(-1, ConeMath.lane(-1, 1));
		assertEquals(1, ConeMath.lane(1, 1));
		assertEquals(0, ConeMath.lane(1, 3));
		assertEquals(1, ConeMath.lane(2, 3));
	}

	@Test
	void weightsFavourStraightAhead() {
		assertEquals(1, ConeMath.weight(0, 5), 1e-9);
		assertEquals(0.5, ConeMath.weight(3, 3), 1e-9);
		assertTrue(ConeMath.weight(1, 5) > ConeMath.weight(2, 5));
	}

	@Test
	void errorIsPositiveToTheRight() {
		assertEquals(45, ConeMath.errorDegrees(1, 1), 1e-4);
		assertEquals(-45, ConeMath.errorDegrees(-1, 1), 1e-4);
		assertEquals(0, ConeMath.errorDegrees(0, 4), 1e-6);
	}

	@Test
	void pursuitTurnFollowsTheCurveOntoTheAim() {
		// No turn for a point straight ahead or inside the dead zone.
		assertEquals(0, ConeMath.pursuitTurn(0, 6, 0.5, 1, 10, 3), 1e-6);
		assertEquals(0, ConeMath.pursuitTurn(0.1, 6, 0.5, 1, 10, 3), 1e-6);
		// Without a dead zone: 0.5 blocks/tick onto a point 3 right and 4 ahead (5 away, sin 0.6) is 0.12 rad/tick.
		assertEquals(Math.toDegrees(0.12), ConeMath.pursuitTurn(3, 4, 0.5, 1, 45, 0), 1e-4);
		assertEquals(-Math.toDegrees(0.12), ConeMath.pursuitTurn(-3, 4, 0.5, 1, 45, 0), 1e-4);
		// Gain scales it, and the gait's turn rate caps it.
		assertEquals(2 * Math.toDegrees(0.12), ConeMath.pursuitTurn(3, 4, 0.5, 2, 45, 0), 1e-4);
		assertEquals(5, ConeMath.pursuitTurn(3, 4, 0.5, 1, 5, 0), 1e-6);
	}

	@Test
	void pursuitTurnIsGentleForFarPointsAndGrowsFromZero() {
		// The same offset further ahead is a gentler turn, so bends are taken early and smoothly.
		assertTrue(ConeMath.pursuitTurn(1, 8, 0.5, 1, 45, 0) < ConeMath.pursuitTurn(1, 3, 0.5, 1, 45, 0));
		// Just past the dead zone the turn is tiny, not a jump.
		double justPast = 6 * Math.tan(Math.toRadians(3.2));
		assertTrue(ConeMath.pursuitTurn(justPast, 6, 0.5, 1, 45, 3) < 0.05);
		// Faster gaits turn harder for the same aim, as they reach it sooner.
		assertTrue(ConeMath.pursuitTurn(2, 6, 0.7, 1, 45, 0) > ConeMath.pursuitTurn(2, 6, 0.2, 1, 45, 0));
	}

	@Test
	void smoothingMovesPartOfTheWay() {
		assertEquals(2.5, ConeMath.smooth(0, 10, 0.25), 1e-6);
		assertEquals(10, ConeMath.smooth(0, 10, 1), 1e-6);
		assertEquals(5, ConeMath.smooth(5, 10, 0), 1e-6);
	}
}
