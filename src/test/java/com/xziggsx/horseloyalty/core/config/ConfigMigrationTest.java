package com.xziggsx.horseloyalty.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xziggsx.horseloyalty.foals.FoalSettings;
import com.xziggsx.horseloyalty.genetics.GeneticsSettings;
import com.xziggsx.horseloyalty.hunger.HungerSettings;
import com.xziggsx.horseloyalty.loyalty.LoyaltySettings;
import com.xziggsx.horseloyalty.personality.PersonalitySettings;
import com.xziggsx.horseloyalty.personality.api.Personality;
import com.xziggsx.horseloyalty.riding.RidingSettings;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Loads a version 3 config (the old single layout, with some values tuned away from the defaults)
 * and checks that version 4 keeps every tuned value in its new place.
 */
class ConfigMigrationTest {
	private static JsonObject v3() throws IOException {
		try (InputStream in = ConfigMigrationTest.class.getResourceAsStream("/config/horseloyalty-v3.json")) {
			assertNotNull(in, "test config missing");
			return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	void enabledFlagsBecomeSwitches() throws IOException {
		JsonObject v4 = ConfigMigration.fromV3(v3()).config();
		JsonObject features = v4.getAsJsonObject("features");
		assertEquals(4, v4.get("configVersion").getAsInt());
		assertEquals("off", features.get("hunger").getAsString());
		assertEquals("off", features.get("foals").getAsString());
		assertEquals("on", features.get("genetics").getAsString());
		assertEquals("on", features.get("personality").getAsString());
		assertEquals("on", features.get("riding").getAsString());
		assertFalse(v4.getAsJsonObject("hunger").has("enabled"));
	}

	@Test
	void tunedValuesSurvive() throws IOException {
		JsonObject v4 = ConfigMigration.fromV3(v3()).config();
		LoyaltySettings loyalty = ConfigFile.parse(LoyaltySettings.class, v4.get("loyalty"));
		assertEquals(3, loyalty.pointsPerFeed());
		assertEquals(50, loyalty.pointsNeeded(3));
		assertEquals(200, loyalty.pointsNeeded(5));

		RidingSettings riding = ConfigFile.parse(RidingSettings.class, v4.get("riding"));
		assertEquals(0.85, riding.topSpeedShare(3), 1e-9);
		assertEquals(6, riding.turnEaseTicks());
		assertEquals(0.06, riding.gaitRaiseFailChance(Gait.WALK, 1), 1e-9);
		assertEquals(600 * 20, riding.gaitDropTicks(5));

		PersonalitySettings personality = ConfigFile.parse(PersonalitySettings.class, v4.get("personality"));
		assertTrue(personality.bucksOff(Personality.LAZY));
		assertTrue(personality.bucksOff(Personality.SKITTISH));
		assertEquals(0.5, personality.buckOffChance(), 1e-9);

		HungerSettings hunger = ConfigFile.parse(HungerSettings.class, v4.get("hunger"));
		assertEquals(600 * 20, hunger.ticksUntilHungry());
		GeneticsSettings genetics = ConfigFile.parse(GeneticsSettings.class, v4.get("genetics"));
		assertEquals(0.025, genetics.mutationChance(), 1e-9);
		FoalSettings foals = ConfigFile.parse(FoalSettings.class, v4.get("foals"));
		assertEquals(0.25, foals.parentLoyaltyShare(), 1e-9);
	}

	@Test
	void feedbackGoesToTheClientPrefs() throws IOException {
		ConfigMigration.Result result = ConfigMigration.fromV3(v3());
		assertNotNull(result.feedback());
		assertFalse(result.feedback().get("gaitSounds").getAsBoolean());
		assertFalse(result.config().has("feedback"));
	}

	@Test
	void versionOneDropsGetTheNewDefaults() throws IOException {
		JsonObject old = v3();
		old.addProperty("configVersion", 1);
		JsonObject level1 = old.getAsJsonArray("loyaltyLevels").get(0).getAsJsonObject();
		level1.addProperty("gaitDropEverySeconds", 30);
		level1.addProperty("gaitDropChancePercent", 60);
		RidingSettings riding = ConfigFile.parse(RidingSettings.class, ConfigMigration.fromV3(old).config().get("riding"));
		assertEquals(45 * 20, riding.gaitDropTicks(1));
		assertEquals(0.4, riding.gaitDropChance(1), 1e-9);
	}

	@Test
	void outOfRangeValuesAreClamped() {
		JsonObject hunger = JsonParser.parseString("{\"secondsUntilHungry\": -5, \"loyaltyPointsLost\": 99999}").getAsJsonObject();
		HungerSettings settings = ConfigFile.parse(HungerSettings.class, hunger);
		assertEquals(20, settings.ticksUntilHungry());
		assertEquals(1000, settings.loyaltyPointsLost());
		// Missing values take their defaults.
		assertEquals(120 * 20, settings.loyaltyLossIntervalTicks());
	}
}
