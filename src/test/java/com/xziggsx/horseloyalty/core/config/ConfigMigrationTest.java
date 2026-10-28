package com.xziggsx.horseloyalty.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xziggsx.horseloyalty.feedback.api.FeedbackSettings;
import com.xziggsx.horseloyalty.foals.FoalSettings;
import com.xziggsx.horseloyalty.genetics.GeneticsSettings;
import com.xziggsx.horseloyalty.hunger.HungerSettings;
import com.xziggsx.horseloyalty.loyalty.LoyaltySettings;
import com.xziggsx.horseloyalty.personality.PersonalitySettings;
import com.xziggsx.horseloyalty.personality.api.Personality;
import com.xziggsx.horseloyalty.riding.RidingSettings;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	/**
	 * Every setting of the old file, each changed away from its default, turns up unchanged in its
	 * new place: in a section of the version 4 file, as a feature switch, or in the client prefs.
	 */
	@Test
	void everyOldSettingHasANewHome() throws IOException {
		JsonObject old = v3();
		tuneEveryNumber(old);
		old.getAsJsonObject("hunger").addProperty("enabled", false);
		ConfigMigration.Result result = ConfigMigration.fromV3(old);
		JsonObject v4 = result.config();
		Map<String, JsonElement> oldValues = new TreeMap<>();
		flatten(old, "", oldValues);
		List<String> lost = new ArrayList<>();
		int checked = 0;
		for (Map.Entry<String, JsonElement> entry : oldValues.entrySet()) {
			String path = entry.getKey();
			if (path.equals("_readme") || path.equals("configVersion")) {
				continue;
			}
			JsonElement expected = entry.getValue();
			JsonElement found;
			if (path.endsWith(".enabled") && List.of("hunger", "foals", "personality", "genetics").contains(path.split("\\.")[0])) {
				found = get(v4, "features." + path.split("\\.")[0]);
				expected = new JsonPrimitive(expected.getAsBoolean() ? "on" : "off");
			} else if (path.startsWith("feedback.")) {
				found = get(result.feedback(), path.substring("feedback.".length()));
			} else {
				found = get(v4, newPath(path));
			}
			checked++;
			if (!expected.equals(found)) {
				lost.add(path + " = " + expected + " -> " + newPath(path) + " = " + found);
			}
		}
		assertTrue(lost.isEmpty(), "Settings lost or changed by the migration:\n" + String.join("\n", lost));
		assertEquals(oldValues.size() - 2, checked);
	}

	/**
	 * Every key the migration writes is one the settings classes read: a key no class has would be
	 * silently ignored, which loses the setting just the same.
	 */
	@Test
	void everyMigratedKeyIsRead() throws IOException {
		ConfigMigration.Result result = ConfigMigration.fromV3(v3());
		Map<String, Class<? extends SettingsSection>> sections = Map.of("riding", RidingSettings.class, "loyalty", LoyaltySettings.class,
				"hunger", HungerSettings.class, "personality", PersonalitySettings.class, "genetics", GeneticsSettings.class,
				"foals", FoalSettings.class);
		List<String> unread = new ArrayList<>();
		sections.forEach((key, type) -> unread.addAll(unreadKeys(key, result.config().get(key), type)));
		unread.addAll(unreadKeys("feedback", result.feedback(), FeedbackSettings.class));
		assertTrue(unread.isEmpty(), "Keys no settings class reads:\n" + String.join("\n", unread));
	}

	/** Keys in {@code json} that a default instance of {@code type} doesn't have, ignoring list positions. */
	private static List<String> unreadKeys(String section, JsonElement json, Class<? extends SettingsSection> type) {
		Map<String, JsonElement> known = new TreeMap<>();
		flatten(new Gson().toJsonTree(SettingsResolver.newInstance(type)), "", known);
		List<String> knownPaths = known.keySet().stream().map(path -> path.replaceAll("\\[\\d+]", "[]")).toList();
		Map<String, JsonElement> written = new TreeMap<>();
		flatten(json, "", written);
		List<String> unread = new ArrayList<>();
		for (String path : written.keySet()) {
			String normalized = path.replaceAll("\\[\\d+]", "[]");
			// A key is read if the class has it, or it sits in a map or list the class reads as a whole.
			boolean read = knownPaths.stream().anyMatch(field -> field.equals(normalized)
					|| normalized.startsWith(field.replaceAll("\\[]$", "") + "."));
			if (!read && !normalized.startsWith("weights.") && !normalized.startsWith("gaitRaise.failChancePercent.")) {
				unread.add(section + "." + path);
			}
		}
		return unread;
	}

	/** Where an old setting lives in version 4. */
	private static String newPath(String old) {
		Matcher level = Pattern.compile("loyaltyLevels\\[(\\d+)]\\.(\\w+)").matcher(old);
		if (level.matches()) {
			int index = Integer.parseInt(level.group(1));
			return level.group(2).equals("pointsNeeded")
					? "loyalty.pointsNeeded[" + index + "]" : "riding.byLoyaltyLevel[" + index + "]." + level.group(2);
		}
		if (old.startsWith("loyaltyGain.")) {
			return "loyalty." + old.substring("loyaltyGain.".length());
		}
		if (old.startsWith("gaitRaise.buckOffPersonalities")) {
			return old.replace("gaitRaise.buckOffPersonalities", "personality.bucksOff");
		}
		if (old.equals("gaitRaise.buckOffPercent")) {
			return "personality.buckOffPercent";
		}
		if (old.startsWith("gaitRaise.") || old.startsWith("gallopPush.") || old.startsWith("gaitSpeedPercent.")
				|| old.equals("turnEaseTicks") || old.equals("sidestepAngleDegrees")) {
			return "riding." + old;
		}
		return old;
	}

	/** Changes every number in the file, so a value that falls back to its default is caught. */
	private static void tuneEveryNumber(JsonElement element) {
		if (element instanceof JsonObject object) {
			for (String key : object.keySet()) {
				JsonElement value = object.get(key);
				if (value instanceof JsonPrimitive primitive && primitive.isNumber() && !key.equals("configVersion") && !key.equals("level")) {
					object.addProperty(key, primitive.getAsDouble() + 1.25);
				} else {
					tuneEveryNumber(value);
				}
			}
		} else if (element instanceof JsonArray array) {
			for (int i = 0; i < array.size(); i++) {
				JsonElement value = array.get(i);
				if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
					array.set(i, new JsonPrimitive(primitive.getAsDouble() + 1.25));
				} else {
					tuneEveryNumber(value);
				}
			}
		}
	}

	/** Every leaf value by its path, such as "loyaltyLevels[2].topSpeedPercent". */
	private static void flatten(JsonElement element, String path, Map<String, JsonElement> out) {
		if (element instanceof JsonObject object) {
			object.entrySet().forEach(entry -> flatten(entry.getValue(), path.isEmpty() ? entry.getKey() : path + "." + entry.getKey(), out));
		} else if (element instanceof JsonArray array) {
			for (int i = 0; i < array.size(); i++) {
				flatten(array.get(i), path + "[" + i + "]", out);
			}
		} else {
			out.put(path, element);
		}
	}

	/** The value at a path, or null. */
	private static JsonElement get(JsonElement root, String path) {
		JsonElement current = root;
		for (String part : path.split("\\.")) {
			Matcher indexed = Pattern.compile("(\\w+)((?:\\[\\d+])*)").matcher(part);
			if (!indexed.matches() || current == null || !current.isJsonObject()) {
				return null;
			}
			current = current.getAsJsonObject().get(indexed.group(1));
			Matcher index = Pattern.compile("\\[(\\d+)]").matcher(indexed.group(2));
			while (index.find()) {
				if (current == null || !current.isJsonArray()) {
					return null;
				}
				int i = Integer.parseInt(index.group(1));
				current = i < current.getAsJsonArray().size() ? current.getAsJsonArray().get(i) : null;
			}
		}
		return current;
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
