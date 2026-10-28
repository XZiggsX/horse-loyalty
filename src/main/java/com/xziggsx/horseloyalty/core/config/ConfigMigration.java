package com.xziggsx.horseloyalty.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;

/**
 * The one-time split of the old single config (versions 0 to 3) into version 4: a "features" block
 * and one section per feature. Keeps everything a player tuned. After this, each section migrates
 * itself by its own version, so core never needs to know a feature's layout again.
 * <p>
 * Works on plain JSON, so it can be tested without the game.
 */
public final class ConfigMigration {
	/** The version this class migrates to. */
	public static final int VERSION = 4;

	/** Old default gait drops of version 1 by level: {seconds, chance}. Version 2 made them rarer. */
	private static final double[][] V1_DROPS = {{30, 60}, {30, 40}, {30, 25}, {30, 15}, {300, 100}};
	/** Version 2's defaults that replaced them, by level: {seconds, chance}. */
	private static final double[][] V2_DROPS = {{45, 40}, {45, 25}, {45, 15}, {45, 8}, {600, 100}};

	/**
	 * The result of migrating.
	 *
	 * @param config   the version 4 config
	 * @param feedback the old "feedback" section for the client prefs file, or null if there was none
	 */
	public record Result(JsonObject config, JsonObject feedback) {
	}

	private ConfigMigration() {
	}

	/** Whether this file still has the old single layout. */
	public static boolean needed(JsonObject old) {
		return version(old) < VERSION;
	}

	/** Turns a file of version 0 to 3 into version 4. Doesn't change {@code old}. */
	public static Result fromV3(JsonObject old) {
		JsonObject in = old.deepCopy();
		int version = version(in);
		JsonArray levels = array(in, "loyaltyLevels");
		if (version < 2 && levels != null) {
			updateV1Drops(levels);
		}

		JsonObject out = new JsonObject();
		out.addProperty("configVersion", VERSION);

		JsonObject features = new JsonObject();
		features.addProperty("riding", "on");
		features.addProperty("loyalty", "on");
		features.addProperty("hunger", switchOf(in, "hunger"));
		features.addProperty("personality", switchOf(in, "personality"));
		features.addProperty("genetics", switchOf(in, "genetics"));
		features.addProperty("foals", switchOf(in, "foals"));
		features.addProperty("summoning", "on");
		features.addProperty("horseInfo", "on");
		features.addProperty("devTools", "on");
		out.add("features", features);

		JsonObject riding = new JsonObject();
		copy(in, "gaitSpeedPercent", riding);
		copy(in, "turnEaseTicks", riding);
		copy(in, "sidestepAngleDegrees", riding);
		JsonObject gaitRaise = object(in, "gaitRaise");
		JsonObject gallopPush = object(in, "gallopPush");

		JsonObject personality = withoutEnabled(object(in, "personality"));
		if (gaitRaise != null) {
			gaitRaise = gaitRaise.deepCopy();
			if (gaitRaise.has("buckOffPersonalities")) {
				personality.add("bucksOff", gaitRaise.remove("buckOffPersonalities"));
			}
			if (gaitRaise.has("buckOffPercent")) {
				personality.add("buckOffPercent", gaitRaise.remove("buckOffPercent"));
			}
		}

		JsonObject loyalty = new JsonObject();
		JsonObject gain = object(in, "loyaltyGain");
		if (gain != null) {
			for (String key : List.of("pointsPerFeed", "feedCooldownSeconds", "ridingSecondsPerPoint")) {
				copy(gain, key, loyalty);
			}
		}
		if (levels != null) {
			loyalty.add("pointsNeeded", pointsNeeded(levels));
			riding.add("byLoyaltyLevel", ridingLevels(levels));
		}
		if (gaitRaise != null) {
			riding.add("gaitRaise", gaitRaise);
		}
		if (gallopPush != null) {
			riding.add("gallopPush", gallopPush.deepCopy());
		}

		out.add("riding", riding);
		out.add("loyalty", loyalty);
		out.add("hunger", withoutEnabled(object(in, "hunger")));
		out.add("personality", personality);
		out.add("genetics", withoutEnabled(object(in, "genetics")));
		out.add("foals", withoutEnabled(object(in, "foals")));
		JsonObject feedback = object(in, "feedback");
		return new Result(out, feedback == null ? null : feedback.deepCopy());
	}

	/** Version 2 made gait drops rarer: levels still at the version 1 defaults get the new ones. */
	private static void updateV1Drops(JsonArray levels) {
		for (JsonElement element : levels) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject level = element.getAsJsonObject();
			int index = (int) number(level, "level", 0) - 1;
			if (index < 0 || index >= V1_DROPS.length) {
				continue;
			}
			if (number(level, "gaitDropEverySeconds", -1) == V1_DROPS[index][0]
					&& number(level, "gaitDropChancePercent", -1) == V1_DROPS[index][1]) {
				level.addProperty("gaitDropEverySeconds", V2_DROPS[index][0]);
				level.addProperty("gaitDropChancePercent", V2_DROPS[index][1]);
			}
		}
	}

	/** Points needed for levels 1 to 5, in order. A level the file doesn't have gets null, so its default applies. */
	private static JsonArray pointsNeeded(JsonArray levels) {
		JsonArray points = new JsonArray();
		for (int level = 1; level <= 5; level++) {
			JsonObject entry = levelEntry(levels, level);
			points.add(entry != null && entry.has("pointsNeeded") ? entry.get("pointsNeeded") : null);
		}
		return points;
	}

	/** The riding half of each level entry: everything but its points. */
	private static JsonArray ridingLevels(JsonArray levels) {
		JsonArray riding = new JsonArray();
		for (JsonElement element : levels) {
			if (element.isJsonObject()) {
				JsonObject entry = element.getAsJsonObject().deepCopy();
				entry.remove("pointsNeeded");
				riding.add(entry);
			}
		}
		return riding;
	}

	private static JsonObject levelEntry(JsonArray levels, int level) {
		for (JsonElement element : levels) {
			if (element.isJsonObject() && number(element.getAsJsonObject(), "level", 0) == level) {
				return element.getAsJsonObject();
			}
		}
		return null;
	}

	/** "off" if the old section said "enabled": false, else "on". */
	private static String switchOf(JsonObject in, String section) {
		JsonObject object = object(in, section);
		boolean on = object == null || !object.has("enabled") || !object.get("enabled").isJsonPrimitive()
				|| object.get("enabled").getAsBoolean();
		return on ? "on" : "off";
	}

	private static JsonObject withoutEnabled(JsonObject section) {
		JsonObject copy = section == null ? new JsonObject() : section.deepCopy();
		copy.remove("enabled");
		return copy;
	}

	private static void copy(JsonObject from, String key, JsonObject to) {
		if (from.has(key)) {
			to.add(key, from.get(key).deepCopy());
		}
	}

	private static int version(JsonObject config) {
		return (int) number(config, "configVersion", 0);
	}

	private static double number(JsonObject object, String key, double fallback) {
		JsonElement element = object.get(key);
		return element instanceof JsonPrimitive primitive && primitive.isNumber() ? primitive.getAsDouble() : fallback;
	}

	private static JsonObject object(JsonObject parent, String key) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static JsonArray array(JsonObject parent, String key) {
		JsonElement element = parent.get(key);
		return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
	}
}
