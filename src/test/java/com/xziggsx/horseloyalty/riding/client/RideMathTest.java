package com.xziggsx.horseloyalty.riding.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RideMathTest {
	@Test
	void turnsEaseInAndOut() {
		// 8 degrees per tick reached over 4 ticks: 2 more each tick, never overshooting.
		float rate = 0;
		for (int tick = 1; tick <= 4; tick++) {
			rate = RideMath.easeTurn(rate, -8, 2);
			assertEquals(-2 * tick, rate, 1e-6);
		}
		assertEquals(-8, RideMath.easeTurn(rate, -8, 2), 1e-6);
		// Letting go eases back toward straight ahead.
		assertEquals(-6, RideMath.easeTurn(-8, 0, 2), 1e-6);
	}

	@Test
	void turnTargets() {
		// A (positive side key) turns left, which lowers the yaw.
		assertEquals(-5, RideMath.turnTarget(false, 0, false, 1, 5), 1e-6);
		assertEquals(5, RideMath.turnTarget(false, 0, false, -1, 5), 1e-6);
		// Sidestepping keeps the heading; a bolting horse ignores the rider.
		assertEquals(0, RideMath.turnTarget(false, 0, true, 1, 5), 1e-6);
		assertEquals(5, RideMath.turnTarget(true, 1, true, -1, 5), 1e-6);
	}

	@Test
	void senseTargetOnlyWithoutKeys() {
		// No keys: the horse's own target, or straight on without one.
		assertEquals(2.5, RideMath.turnTarget(false, 0, false, 0, 5, 2.5f), 1e-6);
		assertEquals(0, RideMath.turnTarget(false, 0, false, 0, 5, Float.NaN), 1e-6);
		// The rider's keys, a sidestep and a bolt all come first.
		assertEquals(-5, RideMath.turnTarget(false, 0, false, 1, 5, 2.5f), 1e-6);
		assertEquals(0, RideMath.turnTarget(false, 0, true, 0, 5, 2.5f), 1e-6);
		assertEquals(-5, RideMath.turnTarget(true, -1, false, 0, 5, 2.5f), 1e-6);
	}

	@Test
	void inputs() {
		assertEquals(-1, RideMath.forwardInput(true, true));
		assertEquals(0, RideMath.forwardInput(false, true));
		assertEquals(1, RideMath.forwardInput(false, false));
		// Halted, a sidestep is straight sideways; moving at 45 degrees, as much sideways as forward.
		assertEquals(1, RideMath.sideInput(0, 1, 45), 1e-9);
		assertEquals(-1, RideMath.sideInput(1, -1, 45), 1e-9);
		assertEquals(Math.tan(Math.toRadians(30)), RideMath.sideInput(1, 1, 30), 1e-9);
	}

	@Test
	void pushEffortAndFade() {
		assertEquals(0.5, RideMath.effort(0.1, 0.2), 1e-9);
		assertEquals(1, RideMath.effort(0.4, 0.2), 1e-9);
		assertEquals(0, RideMath.effort(0.1, 0), 1e-9);
		// The default half-life of one second halves the push in 20 ticks.
		double perTick = Math.pow(0.5, 1 / 20.0);
		double push = 0.2;
		for (int tick = 0; tick < 20; tick++) {
			push = RideMath.fade(push, perTick);
		}
		assertEquals(0.1, push, 1e-9);
	}
}
