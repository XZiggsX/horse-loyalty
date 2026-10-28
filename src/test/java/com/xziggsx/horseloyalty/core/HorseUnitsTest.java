package com.xziggsx.horseloyalty.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HorseUnitsTest {
	@Test
	void speedInBlocksPerSecond() {
		// Same conversion as riding: the fastest vanilla horse about 14.9 blocks per second, the slowest 5.0.
		assertEquals(14.87, HorseUnits.blocksPerSecond(HorseUnits.MAX_SPEED), 0.05);
		assertEquals(4.96, HorseUnits.blocksPerSecond(HorseUnits.MIN_SPEED), 0.05);
	}

	@Test
	void jumpInBlocks() {
		// Vanilla jump strength 0.4 to 1.0 clears about 1.1 to 5.3 blocks.
		assertEquals(1.09, HorseUnits.jumpBlocks(HorseUnits.MIN_JUMP), 0.05);
		assertEquals(5.29, HorseUnits.jumpBlocks(HorseUnits.MAX_JUMP), 0.05);
		assertTrue(HorseUnits.jumpBlocks(0.7) > HorseUnits.jumpBlocks(0.6));
	}

	@Test
	void fillStaysInRange() {
		assertEquals(0.5, HorseUnits.fill(5, 0, 10), 1e-9);
		assertEquals(0, HorseUnits.fill(-3, 0, 10), 1e-9);
		assertEquals(1, HorseUnits.fill(30, 0, 10), 1e-9);
		assertEquals(1, HorseUnits.fill(1, 2, 2), 1e-9);
	}
}
