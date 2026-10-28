package com.xziggsx.horseloyalty.horseinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatUnitsTest {
	@Test
	void speedInBlocksPerSecond() {
		// Same conversion as riding: the fastest vanilla horse about 14.9 blocks per second, the slowest 5.0.
		assertEquals(14.87, StatUnits.blocksPerSecond(StatUnits.MAX_SPEED), 0.05);
		assertEquals(4.96, StatUnits.blocksPerSecond(StatUnits.MIN_SPEED), 0.05);
	}

	@Test
	void jumpInBlocks() {
		// Vanilla jump strength 0.4 to 1.0 clears about 1.1 to 5.3 blocks.
		assertEquals(1.09, StatUnits.jumpBlocks(StatUnits.MIN_JUMP), 0.05);
		assertEquals(5.29, StatUnits.jumpBlocks(StatUnits.MAX_JUMP), 0.05);
		assertTrue(StatUnits.jumpBlocks(0.7) > StatUnits.jumpBlocks(0.6));
	}

	@Test
	void fillStaysInRange() {
		assertEquals(0.5, StatUnits.fill(5, 0, 10), 1e-9);
		assertEquals(0, StatUnits.fill(-3, 0, 10), 1e-9);
		assertEquals(1, StatUnits.fill(30, 0, 10), 1e-9);
		assertEquals(1, StatUnits.fill(1, 2, 2), 1e-9);
	}

	@Test
	void everythingPolicyNeverLocks() {
		for (InfoTopic topic : InfoTopic.values()) {
			assertTrue(RevealPolicies.everything().lockReason(topic, null, null).isEmpty());
		}
	}
}
