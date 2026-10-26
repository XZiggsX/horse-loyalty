package com.xziggsx.horseloyalty.config;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.personality.Personality;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * User config in {@code config/horseloyalty.json}. Written with the defaults on first
 * launch; read once at startup, so changes need a game restart.
 * <p>
 * Loaded on both sides from the same file. Loyalty gain, level thresholds and hunger are
 * used by the server. Speed caps, turning and gait drops are used by the rider's client,
 * because riding runs there.
 * <p>
 * Field names here are the JSON keys, so renaming a field breaks existing config files.
 */
public final class HorseLoyaltyConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String FILE_NAME = HorseLoyaltyMod.MOD_ID + ".json";

	private static HorseLoyaltyConfig instance = defaults();

	// --- JSON fields -------------------------------------------------------------

	@SuppressWarnings("unused")
	private String _readme = "loyaltyGain: how fast loyalty builds. Each feed gives pointsPerFeed, "
			+ "but only once per feedCooldownSeconds per player. Riding a moving horse gives 1 point "
			+ "per ridingSecondsPerPoint. loyaltyLevels (1 Unruly to 5 Partners): pointsNeeded is the "
			+ "total points to reach that level (level 1 is just tamed). topSpeedPercent is the most of "
			+ "the horse's normal speed it will run for you; every gait is scaled by it. gaitSpeedPercent: "
			+ "each gait's share of the horse's top speed, before the loyalty scaling. sidestepAngleDegrees: "
			+ "how far off straight ahead the horse moves while you hold sprint with A or D. "
			+ "turnDegreesPerTick is how fast A/D turn it; double it to halve the turn circle. "
			+ "turnEaseTicks is how long it takes to reach full turn rate. While moving, every "
			+ "gaitDropEverySeconds without the rider shifting gait, the horse has gaitDropChancePercent "
			+ "chance to drop one gait on its own (down to a halt), so riders can't go AFK. Use 100 for a "
			+ "fixed timer, 0 to never drop. hunger: a tamed horse that hasn't been fed or left to graze "
			+ "(unridden, on grass, mycelium or podzol) for secondsUntilHungry shows dark particles at its "
			+ "hooves and loses loyaltyPointsLost points with every rider each loyaltyLossEverySeconds. "
			+ "It never drops below level 1 or becomes untamed. foals: a foal bred by a player is born tamed "
			+ "to them, with parentLoyaltyPercent of the average of their points with the two parents. It picks "
			+ "one parent at random to follow (roleplay it as mother or father) once more than "
			+ "followParentDistance blocks away, up to followParentMaxDistance. Without that parent near, it "
			+ "follows its breeder the same way. It skips strollSkipPercent of its random strolls and never "
			+ "strolls further than strollMaxDistance from that parent or player. Foals that spawn in the wild "
			+ "act as in vanilla. personality: every horse gets one, rolled with the weights (steady has no "
			+ "quirk; brave only panics when burning; skittish flees creepers within skittishFleeDistance and, "
			+ "ridden, bolts at a gallop for boltSeconds when one is within boltCreeperDistance, half as long at "
			+ "loyalty 4 and never at 5; curious wanders toward a village bell within curiousSearchRadius about "
			+ "every curiousWanderEverySeconds when loose; lazy multiplies the gait drop chance and time by the "
			+ "lazy multipliers). You see it in the horse screen from revealAtLevel. A foal gets each parent's "
			+ "personality with inheritPercent chance (the rest is a fresh roll), or sameParentsPercent when both "
			+ "parents share one. Restart the game after editing.";

	private LoyaltyGain loyaltyGain = new LoyaltyGain();

	private HungerSettings hunger = new HungerSettings();

	private FoalSettings foals = new FoalSettings();

	private PersonalitySettings personality = new PersonalitySettings();

	private int turnEaseTicks = 4;

	private double sidestepAngleDegrees = 45;

	private GaitSpeeds gaitSpeedPercent = new GaitSpeeds();

	private List<LevelSettings> loyaltyLevels = defaultLevels();

	public static final class LoyaltyGain {
		private int pointsPerFeed = 2;
		private double feedCooldownSeconds = 30;
		private double ridingSecondsPerPoint = 60;
	}

	public static final class HungerSettings {
		private boolean enabled = true;
		private double secondsUntilHungry = 1200;
		private double loyaltyLossEverySeconds = 120;
		private int loyaltyPointsLost = 1;
	}

	public static final class GaitSpeeds {
		private double walk = 30;
		private double trot = 50;
		private double canter = 75;
		private double gallop = 100;
	}

	public static final class FoalSettings {
		private boolean enabled = true;
		private double parentLoyaltyPercent = 25;
		private double followParentDistance = 5;
		private double followParentMaxDistance = 24;
		private double followPlayerDistance = 4;
		private double followPlayerMaxDistance = 16;
		private double strollSkipPercent = 50;
		private double strollMaxDistance = 6;
	}

	public static final class PersonalitySettings {
		private boolean enabled = true;
		private int revealAtLevel = 2;
		private Map<String, Double> weights = defaultWeights();
		private double inheritPercent = 40;
		private double sameParentsPercent = 70;
		private double skittishFleeDistance = 8;
		private double boltCreeperDistance = 6;
		private double boltSeconds = 2.5;
		private int curiousSearchRadius = 48;
		private double curiousWanderEverySeconds = 300;
		private double lazyGaitDropChanceMultiplier = 1.5;
		private double lazyGaitDropTimeMultiplier = 0.7;

		private static Map<String, Double> defaultWeights() {
			Map<String, Double> weights = new LinkedHashMap<>();
			weights.put("steady", 40.0);
			weights.put("brave", 15.0);
			weights.put("skittish", 15.0);
			weights.put("curious", 15.0);
			weights.put("lazy", 15.0);
			return weights;
		}
	}

	public static final class LevelSettings {
		private int level;
		private int pointsNeeded;
		private double topSpeedPercent;
		private double turnDegreesPerTick;
		// Boxed so a missing key (null) can be told apart from 0 (never drop).
		private Double gaitDropEverySeconds;
		private Double gaitDropChancePercent;

		LevelSettings(int level, int pointsNeeded, double topSpeedPercent, double turnDegreesPerTick,
				double gaitDropEverySeconds, double gaitDropChancePercent) {
			this.level = level;
			this.pointsNeeded = pointsNeeded;
			this.topSpeedPercent = topSpeedPercent;
			this.turnDegreesPerTick = turnDegreesPerTick;
			this.gaitDropEverySeconds = gaitDropEverySeconds;
			this.gaitDropChancePercent = gaitDropChancePercent;
		}
	}

	/** Default settings for levels 1 to 5. Also the fallback for missing values. */
	private static List<LevelSettings> defaultLevels() {
		List<LevelSettings> levels = new ArrayList<>();
		// Levels 1 to 4 roll every 30 s, averaging one drop every 50 s, 75 s, 2 min and
		// 3 min 20 s. Level 5 never drops by chance, only on a fixed 5 minute timer.
		levels.add(new LevelSettings(1, 0, 60, 4.0, 30, 60));
		levels.add(new LevelSettings(2, 10, 70, 5.0, 30, 40));
		levels.add(new LevelSettings(3, 40, 80, 6.0, 30, 25));
		levels.add(new LevelSettings(4, 100, 90, 7.0, 30, 15));
		levels.add(new LevelSettings(5, 200, 100, 8.0, 300, 100));
		return levels;
	}

	// --- Resolved values, indexed by level 0 to 5 --------------------------------

	private transient double[] topSpeedShare;
	private transient double[] gaitShares;
	private transient float[] turnDegrees;
	private transient int[] pointsNeeded;
	private transient double[] gaitDropChance;
	private transient int[] gaitDropTicks;
	private transient int pointsPerFeed;
	private transient long feedCooldownTicks;
	private transient int ridingTicksPerPoint;
	private transient boolean hungerEnabled;
	private transient long ticksUntilHungry;
	private transient long loyaltyLossIntervalTicks;
	private transient int loyaltyPointsLost;
	private transient FoalSettings resolvedFoals;
	private transient PersonalitySettings resolvedPersonality;

	/** The loaded config. Holds the defaults until {@link #load()} has run. */
	public static HorseLoyaltyConfig get() {
		return instance;
	}

	/** Top speed as a share (0.6 = 60%) for a loyalty level. */
	public double topSpeedShare(int level) {
		return topSpeedShare[Math.clamp(level, 0, Loyalty.MAX_LEVEL)];
	}

	/** A gait's share (0.5 = 50%) of the horse's top speed, by gait index (0 halt to 4 gallop). */
	public double gaitSpeedShare(int gait) {
		return gaitShares[Math.clamp(gait, 0, gaitShares.length - 1)];
	}

	/** Top A/D turn rate for a loyalty level, in degrees per tick. */
	public float turnDegreesPerTick(int level) {
		return turnDegrees[Math.clamp(level, 0, Loyalty.MAX_LEVEL)];
	}

	/** Angle off straight ahead while sidestepping (sprint key + A/D), in degrees. */
	public double sidestepAngleDegrees() {
		return sidestepAngleDegrees;
	}

	/** Ticks to go from no turn to the full turn rate. */
	public int turnEaseTicks() {
		return turnEaseTicks;
	}

	/** Total points needed to reach a level. Levels 0 and 1 need none. */
	public int pointsNeeded(int level) {
		return pointsNeeded[Math.clamp(level, 0, Loyalty.MAX_LEVEL)];
	}

	/** Chance (0.1 = 10%) per check that the horse drops one gait on its own. */
	public double gaitDropChance(int level) {
		return gaitDropChance[Math.clamp(level, 0, Loyalty.MAX_LEVEL)];
	}

	/** Ticks of riding without shifting gait between gait drop rolls. */
	public int gaitDropTicks(int level) {
		return gaitDropTicks[Math.clamp(level, 0, Loyalty.MAX_LEVEL)];
	}

	/** Loyalty points for each counted feed. */
	public int pointsPerFeed() {
		return pointsPerFeed;
	}

	/** Minimum ticks between two feeds that count, per player. */
	public long feedCooldownTicks() {
		return feedCooldownTicks;
	}

	/** Ticks of riding a moving horse per loyalty point. Always at least 1. */
	public int ridingTicksPerPoint() {
		return ridingTicksPerPoint;
	}

	/** Whether hunger (and the loyalty loss it causes) is on. */
	public boolean hungerEnabled() {
		return hungerEnabled;
	}

	/** Ticks without eating or grazing before a horse is hungry. */
	public long ticksUntilHungry() {
		return ticksUntilHungry;
	}

	/** Ticks between loyalty losses while a horse is hungry. */
	public long loyaltyLossIntervalTicks() {
		return loyaltyLossIntervalTicks;
	}

	/** Points every rider loses at each hunger loss. */
	public int loyaltyPointsLost() {
		return loyaltyPointsLost;
	}

	/** Whether bred foals get a family (tamed at birth, follow a parent and their breeder). */
	public boolean foalsEnabled() {
		return resolvedFoals.enabled;
	}

	/** Share (0.25 = 25%) of the breeder's average points with the parents that a foal starts with. */
	public double foalParentLoyaltyShare() {
		return resolvedFoals.parentLoyaltyPercent / 100.0;
	}

	/** Blocks from its parent before a foal starts following. */
	public double foalFollowParentDistance() {
		return resolvedFoals.followParentDistance;
	}

	/** Beyond this many blocks a foal gives up following its parent. */
	public double foalFollowParentMaxDistance() {
		return resolvedFoals.followParentMaxDistance;
	}

	/** Blocks from its breeder before a foal starts following them. */
	public double foalFollowPlayerDistance() {
		return resolvedFoals.followPlayerDistance;
	}

	/** Beyond this many blocks a foal gives up following its breeder. */
	public double foalFollowPlayerMaxDistance() {
		return resolvedFoals.followPlayerMaxDistance;
	}

	/** Chance (0.5 = 50%) that a foal skips a random stroll. */
	public double foalStrollSkipChance() {
		return resolvedFoals.strollSkipPercent / 100.0;
	}

	/** Foals don't stroll further than this from their parent or breeder. */
	public double foalStrollMaxDistance() {
		return resolvedFoals.strollMaxDistance;
	}

	/** Whether horses have personalities. Off means every horse acts steady (vanilla). */
	public boolean personalityEnabled() {
		return resolvedPersonality.enabled;
	}

	/** Loyalty level at which a player sees a horse's personality. */
	public int personalityRevealLevel() {
		return resolvedPersonality.revealAtLevel;
	}

	/** Relative odds of rolling this personality. */
	public double personalityWeight(Personality personality) {
		return resolvedPersonality.weights.getOrDefault(personality.id(), 0.0);
	}

	/** Chance (0.4 = 40%) a foal gets one given parent's personality. */
	public double personalityInheritChance() {
		return resolvedPersonality.inheritPercent / 100.0;
	}

	/** Chance a foal gets its parents' personality when both share it. */
	public double personalitySameParentsChance() {
		return resolvedPersonality.sameParentsPercent / 100.0;
	}

	/** Blocks within which an unridden skittish horse runs from a creeper. */
	public double skittishFleeDistance() {
		return resolvedPersonality.skittishFleeDistance;
	}

	/** Blocks within which a creeper makes a ridden skittish horse bolt. */
	public double boltCreeperDistance() {
		return resolvedPersonality.boltCreeperDistance;
	}

	/** Ticks a skittish horse bolts for, at loyalty below 4. */
	public int boltTicks() {
		return (int) Math.round(resolvedPersonality.boltSeconds * 20);
	}

	/** Blocks a curious horse looks for a village bell in. */
	public int curiousSearchRadius() {
		return resolvedPersonality.curiousSearchRadius;
	}

	/** Average ticks between a loose curious horse's trips toward a village. */
	public int curiousWanderEveryTicks() {
		return (int) Math.round(resolvedPersonality.curiousWanderEverySeconds * 20);
	}

	/** A lazy horse's gait drop chance is multiplied by this. */
	public double lazyGaitDropChanceMultiplier() {
		return resolvedPersonality.lazyGaitDropChanceMultiplier;
	}

	/** A lazy horse's time between gait drop rolls is multiplied by this. */
	public double lazyGaitDropTimeMultiplier() {
		return resolvedPersonality.lazyGaitDropTimeMultiplier;
	}

	// --- Loading -----------------------------------------------------------------

	/** A config with only default values, resolved and ready to use. */
	private static HorseLoyaltyConfig defaults() {
		HorseLoyaltyConfig config = new HorseLoyaltyConfig();
		config.resolve();
		return config;
	}

	/** Reads the config file, or writes it with the defaults if it doesn't exist yet. Called once at startup. */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		HorseLoyaltyConfig config = new HorseLoyaltyConfig();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				HorseLoyaltyConfig read = GSON.fromJson(reader, HorseLoyaltyConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (IOException | JsonParseException e) {
				// Keep the user's broken file so they can fix it; just run on defaults.
				HorseLoyaltyMod.LOGGER.error("Could not read {}, using defaults: {}", path, e.getMessage());
			}
		} else {
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(config, writer);
			} catch (IOException e) {
				HorseLoyaltyMod.LOGGER.warn("Could not write default config {}: {}", path, e.getMessage());
			}
		}
		config.resolve();
		instance = config;
	}

	/** Fills the per-level arrays, falling back to defaults for missing or nonsense values. */
	private void resolve() {
		LoyaltyGain gain = loyaltyGain != null ? loyaltyGain : new LoyaltyGain();
		pointsPerFeed = Math.max(0, gain.pointsPerFeed);
		feedCooldownTicks = Math.round(Math.max(0, gain.feedCooldownSeconds) * 20);
		// At least one tick per point; 0 or less would divide by zero.
		ridingTicksPerPoint = (int) Math.max(1, Math.round(gain.ridingSecondsPerPoint * 20));

		// Halt, walk, trot, canter, gallop. Each gait at least 5% faster than the one below.
		GaitSpeeds speeds = gaitSpeedPercent != null ? gaitSpeedPercent : new GaitSpeeds();
		double[] percents = {0, speeds.walk, speeds.trot, speeds.canter, speeds.gallop};
		gaitShares = new double[percents.length];
		for (int gait = 1; gait < percents.length; gait++) {
			double percent = Math.clamp(percents[gait], 5, 200);
			gaitShares[gait] = Math.max(percent / 100.0, gaitShares[gait - 1] + 0.05);
		}

		HungerSettings hungerSettings = hunger != null ? hunger : new HungerSettings();
		hungerEnabled = hungerSettings.enabled;
		ticksUntilHungry = Math.round(Math.max(1, hungerSettings.secondsUntilHungry) * 20);
		loyaltyLossIntervalTicks = Math.round(Math.max(1, hungerSettings.loyaltyLossEverySeconds) * 20);
		loyaltyPointsLost = Math.max(0, hungerSettings.loyaltyPointsLost);

		// Copied with every value clamped, so a missing or broken section can't break the AI.
		FoalSettings foalSettings = foals != null ? foals : new FoalSettings();
		resolvedFoals = new FoalSettings();
		resolvedFoals.enabled = foalSettings.enabled;
		resolvedFoals.parentLoyaltyPercent = Math.clamp(foalSettings.parentLoyaltyPercent, 0, 100);
		resolvedFoals.followParentDistance = Math.clamp(foalSettings.followParentDistance, 2, 64);
		resolvedFoals.followParentMaxDistance = Math.clamp(foalSettings.followParentMaxDistance,
				resolvedFoals.followParentDistance, 64);
		resolvedFoals.followPlayerDistance = Math.clamp(foalSettings.followPlayerDistance, 2, 64);
		resolvedFoals.followPlayerMaxDistance = Math.clamp(foalSettings.followPlayerMaxDistance,
				resolvedFoals.followPlayerDistance, 64);
		resolvedFoals.strollSkipPercent = Math.clamp(foalSettings.strollSkipPercent, 0, 100);
		resolvedFoals.strollMaxDistance = Math.clamp(foalSettings.strollMaxDistance, 1, 64);

		PersonalitySettings personalitySettings = personality != null ? personality : new PersonalitySettings();
		resolvedPersonality = new PersonalitySettings();
		resolvedPersonality.enabled = personalitySettings.enabled;
		resolvedPersonality.revealAtLevel = Math.clamp(personalitySettings.revealAtLevel, 1, Loyalty.MAX_LEVEL);
		// Missing personalities keep their default weight; negative weights count as 0.
		Map<String, Double> weights = PersonalitySettings.defaultWeights();
		if (personalitySettings.weights != null) {
			personalitySettings.weights.forEach((id, weight) -> {
				if (weights.containsKey(id) && weight != null) {
					weights.put(id, Math.max(0, weight));
				}
			});
		}
		resolvedPersonality.weights = weights;
		resolvedPersonality.inheritPercent = Math.clamp(personalitySettings.inheritPercent, 0, 50);
		resolvedPersonality.sameParentsPercent = Math.clamp(personalitySettings.sameParentsPercent, 0, 100);
		resolvedPersonality.skittishFleeDistance = Math.clamp(personalitySettings.skittishFleeDistance, 1, 32);
		resolvedPersonality.boltCreeperDistance = Math.clamp(personalitySettings.boltCreeperDistance, 1, 32);
		resolvedPersonality.boltSeconds = Math.clamp(personalitySettings.boltSeconds, 0, 30);
		resolvedPersonality.curiousSearchRadius = Math.clamp(personalitySettings.curiousSearchRadius, 8, 128);
		resolvedPersonality.curiousWanderEverySeconds = Math.clamp(personalitySettings.curiousWanderEverySeconds, 10, 86400);
		resolvedPersonality.lazyGaitDropChanceMultiplier = Math.clamp(personalitySettings.lazyGaitDropChanceMultiplier, 0, 10);
		resolvedPersonality.lazyGaitDropTimeMultiplier = Math.clamp(personalitySettings.lazyGaitDropTimeMultiplier, 0.1, 10);

		List<LevelSettings> defaults = defaultLevels();
		topSpeedShare = new double[Loyalty.MAX_LEVEL + 1];
		turnDegrees = new float[Loyalty.MAX_LEVEL + 1];
		pointsNeeded = new int[Loyalty.MAX_LEVEL + 1];
		gaitDropChance = new double[Loyalty.MAX_LEVEL + 1];
		gaitDropTicks = new int[Loyalty.MAX_LEVEL + 1];
		for (int level = 1; level <= Loyalty.MAX_LEVEL; level++) {
			LevelSettings fallback = defaults.get(level - 1);
			LevelSettings settings = find(level);
			if (level >= 2) {
				int points = settings != null && settings.pointsNeeded > 0 ? settings.pointsNeeded : fallback.pointsNeeded;
				// Each level needs at least one point more than the one below.
				pointsNeeded[level] = Math.max(points, pointsNeeded[level - 1] + 1);
			}
			double speed = settings != null && settings.topSpeedPercent > 0 ? settings.topSpeedPercent : fallback.topSpeedPercent;
			double turn = settings != null && settings.turnDegreesPerTick > 0 ? settings.turnDegreesPerTick : fallback.turnDegreesPerTick;
			// 0 is a valid choice here (never drop), so only missing or negative values fall back.
			double drop = settings != null && settings.gaitDropChancePercent != null && settings.gaitDropChancePercent >= 0
					? settings.gaitDropChancePercent : fallback.gaitDropChancePercent;
			gaitDropChance[level] = Math.min(drop, 100) / 100.0;
			double every = settings != null && settings.gaitDropEverySeconds != null && settings.gaitDropEverySeconds > 0
					? settings.gaitDropEverySeconds : fallback.gaitDropEverySeconds;
			gaitDropTicks[level] = (int) Math.max(20, Math.round(every * 20));
			topSpeedShare[level] = Math.min(speed, 200) / 100.0;
			turnDegrees[level] = (float) Math.min(turn, 45);
		}
		// Untamed horses can't be steered; mirror level 1 just in case.
		topSpeedShare[0] = topSpeedShare[1];
		turnDegrees[0] = turnDegrees[1];
		gaitDropChance[0] = gaitDropChance[1];
		gaitDropTicks[0] = gaitDropTicks[1];
		turnEaseTicks = Math.clamp(turnEaseTicks, 1, 100);
		sidestepAngleDegrees = Math.clamp(sidestepAngleDegrees, 5, 85);
	}

	/** The user's settings for a level, or null if the file has none. */
	private LevelSettings find(int level) {
		if (loyaltyLevels == null) {
			return null;
		}
		for (LevelSettings settings : loyaltyLevels) {
			if (settings != null && settings.level == level) {
				return settings;
			}
		}
		return null;
	}
}
