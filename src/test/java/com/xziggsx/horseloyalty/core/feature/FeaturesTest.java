package com.xziggsx.horseloyalty.core.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * How switches and requirements resolve, with stand-in features shaped like the real ones: hunger
 * needs loyalty, and one feature in a loop of requirements. Features is global, so the ids here
 * are their own.
 */
class FeaturesTest {
	private static final String LOYALTY = "test.loyalty";
	private static final String HUNGER = "test.hunger";
	private static final String LOOP_A = "test.loopA";
	private static final String LOOP_B = "test.loopB";

	/** A feature that only has an id and requirements. */
	private record Stub(String id, Set<String> requires) implements Feature {
		@Override
		public void registerData() {
		}

		@Override
		public void wire(FeatureContext context) {
		}
	}

	static {
		Features.register(new Stub(LOYALTY, Set.of()));
		Features.register(new Stub(HUNGER, Set.of(LOYALTY)));
		Features.register(new Stub(LOOP_A, Set.of(LOOP_B)));
		Features.register(new Stub(LOOP_B, Set.of(LOOP_A)));
	}

	@Test
	void everythingIsOnByDefault() {
		Features.useLocal(Map.of());
		assertTrue(Features.isOn(LOYALTY));
		assertTrue(Features.isOn(HUNGER));
		assertFalse(Features.isOn("test.unknown"));
	}

	@Test
	void hungerFollowsLoyalty() {
		Features.useLocal(Map.of(LOYALTY, FeatureSwitch.OFF));
		assertFalse(Features.isOn(LOYALTY));
		// Hunger's own switch is on, but it needs loyalty.
		assertEquals(FeatureSwitch.ON, Features.switchOf(HUNGER));
		assertFalse(Features.isOn(HUNGER));
		assertEquals(Optional.of(LOYALTY), Features.offBecause(HUNGER));

		// Loyalty coming back on brings hunger back.
		Features.useLocal(Map.of(LOYALTY, FeatureSwitch.ON));
		assertTrue(Features.isOn(HUNGER));
		assertEquals(Optional.empty(), Features.offBecause(HUNGER));
	}

	@Test
	void ownSwitchOffIsNotARequirement() {
		Features.useLocal(Map.of(HUNGER, FeatureSwitch.OFF));
		assertFalse(Features.isOn(HUNGER));
		assertTrue(Features.isOn(LOYALTY));
		assertEquals(Optional.empty(), Features.offBecause(HUNGER));
	}

	@Test
	void requirementLoopsCountAsOff() {
		Features.useLocal(Map.of());
		assertFalse(Features.isOn(LOOP_A));
		assertFalse(Features.isOn(LOOP_B));
	}

	@Test
	void aClientFollowsTheServer() {
		Features.useLocal(Map.of());
		Features.useRemote(Map.of(LOYALTY, FeatureSwitch.OFF));
		assertFalse(Features.isOn(HUNGER));
		// Disconnecting goes back to this side's own config.
		Features.useRemote(null);
		assertTrue(Features.isOn(HUNGER));
	}
}
