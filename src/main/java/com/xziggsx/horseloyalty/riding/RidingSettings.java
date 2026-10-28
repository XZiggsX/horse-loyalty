package com.xziggsx.horseloyalty.riding;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.core.config.Synced;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import com.xziggsx.horseloyalty.riding.api.Gait;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The riding section of the config: gait speeds, steering, and per loyalty level the top speed,
 * turn rate and gait drops, plus gait raise and the gallop push. Synced: riding runs on the
 * rider's client, so it rides by the server's numbers. Field names are the JSON keys.
 */
@Synced
@SuppressWarnings("FieldMayBeFinal")
public final class RidingSettings implements SettingsSection {
	@Doc("Each gait's share of the horse's top speed, before the loyalty scaling. Each gait is at least 5% faster than the one below.")
	private GaitSpeeds gaitSpeedPercent = new GaitSpeeds();

	@Range(min = 1, max = 100)
	@Doc("Ticks it takes to reach the full turn rate.")
	private int turnEaseTicks = 4;

	@Range(min = 5, max = 85)
	@Doc("How far off straight ahead the horse moves while you hold sprint with A or D.")
	private double sidestepAngleDegrees = 45;

	@Doc("Per loyalty level 1 to 5: topSpeedPercent is the most of the horse's normal speed it will run for you; turnDegreesPerTick is how fast A/D turn it (double it to halve the turn circle); every gaitDropEverySeconds without the rider shifting gait, the horse has gaitDropChancePercent chance to drop one gait on its own. Use 100 for a fixed timer, 0 to never drop.")
	private List<LevelSettings> byLoyaltyLevel = defaultLevels();

	@Doc("Asking for a faster gait can fail.")
	private GaitRaiseSettings gaitRaise = new GaitRaiseSettings();

	@Doc("Tap W again at a gallop to push the horse faster.")
	private GallopPushSettings gallopPush = new GallopPushSettings();

	private transient double[] gaitShares;
	private transient double[] topSpeedShare;
	private transient float[] turnDegrees;
	private transient double[] gaitDropChance;
	private transient int[] gaitDropTicks;
	/** Fail chance (0.06 = 6%) by gait index (0 halt to 4 gallop), then loyalty level 0 to 5. */
	private transient double[][] gaitRaiseFailChance;

	/** Each gait's share of top speed, in percent. */
	public static final class GaitSpeeds {
		@Range(min = 5, max = 200)
		private double walk = 30;
		@Range(min = 5, max = 200)
		private double trot = 50;
		@Range(min = 5, max = 200)
		private double canter = 75;
		@Range(min = 5, max = 200)
		private double gallop = 100;
	}

	/** One loyalty level's riding numbers. Boxed, so a missing key can be told apart from 0 (never drop). */
	public static final class LevelSettings {
		private Integer level;
		private Double topSpeedPercent;
		private Double turnDegreesPerTick;
		private Double gaitDropEverySeconds;
		private Double gaitDropChancePercent;

		private LevelSettings() {
		}

		LevelSettings(int level, double topSpeedPercent, double turnDegreesPerTick, double gaitDropEverySeconds,
				double gaitDropChancePercent) {
			this.level = level;
			this.topSpeedPercent = topSpeedPercent;
			this.turnDegreesPerTick = turnDegreesPerTick;
			this.gaitDropEverySeconds = gaitDropEverySeconds;
			this.gaitDropChancePercent = gaitDropChancePercent;
		}
	}

	/** Shifting up can fail, and the horse then prances. */
	public static final class GaitRaiseSettings {
		@Doc("Whether shifting up can fail at all.")
		private boolean enabled = true;

		@Range(min = 0, max = 600)
		@Doc("After the horse balks you can't shift up for this many seconds.")
		private double cooldownSeconds = 10;

		@Doc("Per gait you shift up from, the chance for loyalty levels 1 to 5 (by default only from a walk). Add \"gallop\" to make pushing risky too.")
		private Map<String, List<Double>> failChancePercent = defaultFailChances();
	}

	/** Pushing at a gallop with W taps. */
	public static final class GallopPushSettings {
		@Doc("Whether tapping W at a gallop pushes the horse faster.")
		private boolean enabled = true;

		@Range(min = 0, max = 100)
		@Doc("Extra speed per tap.")
		private double percentPerPush = 2;

		@Range(min = 100, max = 300)
		@Doc("The most of its gallop speed all taps together reach.")
		private double maxPercent = 120;

		@Range(min = 0, max = 60)
		@Doc("Stop tapping this long and the extra speed fades away.")
		private double holdSeconds = 1.5;

		@Range(min = 0.05, max = 60)
		@Doc("While fading, the extra speed halves this often. Changing gait ends it at once.")
		private double fadeHalfLifeSeconds = 1;

		@Range(min = 0, max = 100)
		@Doc("Volume of the hard breathing while pushed, 0 for none.")
		private double soundVolumePercent = 40;
	}

	/** Levels 1 to 4 roll every 45 s; level 5 never drops by chance, only on a fixed 10 minute timer. */
	private static List<LevelSettings> defaultLevels() {
		List<LevelSettings> levels = new ArrayList<>();
		levels.add(new LevelSettings(1, 60, 4.0, 45, 40));
		levels.add(new LevelSettings(2, 70, 5.0, 45, 25));
		levels.add(new LevelSettings(3, 80, 6.0, 45, 15));
		levels.add(new LevelSettings(4, 90, 7.0, 45, 8));
		levels.add(new LevelSettings(5, 100, 8.0, 600, 100));
		return levels;
	}

	/** Only shifting up from a walk can fail by default, and only for a horse that barely knows you. */
	private static Map<String, List<Double>> defaultFailChances() {
		Map<String, List<Double>> chances = new LinkedHashMap<>();
		chances.put("walk", List.of(6.0, 3.0, 0.5, 0.0, 0.0));
		return chances;
	}

	/** The section in use: the server's while connected to one with the mod. */
	public static RidingSettings get() {
		return ConfigFile.section(RidingSettings.class);
	}

	@Override
	public String key() {
		return "riding";
	}

	@Override
	public void resolve() {
		double[] percents = {0, gaitSpeedPercent.walk, gaitSpeedPercent.trot, gaitSpeedPercent.canter, gaitSpeedPercent.gallop};
		gaitShares = new double[percents.length];
		for (int gait = 1; gait < percents.length; gait++) {
			gaitShares[gait] = Math.max(percents[gait] / 100.0, gaitShares[gait - 1] + 0.05);
		}

		List<LevelSettings> defaults = defaultLevels();
		topSpeedShare = new double[Bond.MAX_LEVEL + 1];
		turnDegrees = new float[Bond.MAX_LEVEL + 1];
		gaitDropChance = new double[Bond.MAX_LEVEL + 1];
		gaitDropTicks = new int[Bond.MAX_LEVEL + 1];
		for (int level = 1; level <= Bond.MAX_LEVEL; level++) {
			LevelSettings fallback = defaults.get(level - 1);
			LevelSettings settings = find(level);
			double speed = settings != null && settings.topSpeedPercent != null && settings.topSpeedPercent > 0
					? settings.topSpeedPercent : fallback.topSpeedPercent;
			double turn = settings != null && settings.turnDegreesPerTick != null && settings.turnDegreesPerTick > 0
					? settings.turnDegreesPerTick : fallback.turnDegreesPerTick;
			// 0 is a valid choice here (never drop), so only missing or negative values fall back.
			double drop = settings != null && settings.gaitDropChancePercent != null && settings.gaitDropChancePercent >= 0
					? settings.gaitDropChancePercent : fallback.gaitDropChancePercent;
			double every = settings != null && settings.gaitDropEverySeconds != null && settings.gaitDropEverySeconds > 0
					? settings.gaitDropEverySeconds : fallback.gaitDropEverySeconds;
			topSpeedShare[level] = Math.min(speed, 200) / 100.0;
			turnDegrees[level] = (float) Math.min(turn, 45);
			gaitDropChance[level] = Math.min(drop, 100) / 100.0;
			gaitDropTicks[level] = (int) Math.max(20, Math.round(every * 20));
		}
		// Untamed horses can't be steered; mirror level 1 just in case.
		topSpeedShare[0] = topSpeedShare[1];
		turnDegrees[0] = turnDegrees[1];
		gaitDropChance[0] = gaitDropChance[1];
		gaitDropTicks[0] = gaitDropTicks[1];

		Gait[] gaits = Gait.values();
		gaitRaiseFailChance = new double[gaits.length][Bond.MAX_LEVEL + 1];
		for (Gait gait : gaits) {
			List<Double> byLevel = gaitRaise.failChancePercent.get(gait.id());
			// A missing level counts as 0; level 0 (untamed) mirrors level 1.
			for (int level = 1; level <= Bond.MAX_LEVEL; level++) {
				Double percent = byLevel != null && level <= byLevel.size() ? byLevel.get(level - 1) : null;
				gaitRaiseFailChance[gait.ordinal()][level] = percent == null ? 0 : Math.clamp(percent, 0, 100) / 100.0;
			}
			gaitRaiseFailChance[gait.ordinal()][0] = gaitRaiseFailChance[gait.ordinal()][1];
		}
	}

	/** The user's settings for a level, or null if the file has none. */
	private LevelSettings find(int level) {
		for (LevelSettings settings : byLoyaltyLevel) {
			if (settings != null && settings.level != null && settings.level == level) {
				return settings;
			}
		}
		return null;
	}

	/** A gait's share (0.5 = 50%) of the horse's top speed. */
	public double gaitSpeedShare(Gait gait) {
		return gaitShares[gait.ordinal()];
	}

	/** Top speed as a share (0.6 = 60%) for a loyalty level. */
	public double topSpeedShare(int level) {
		return topSpeedShare[Math.clamp(level, 0, Bond.MAX_LEVEL)];
	}

	/** Top A/D turn rate for a loyalty level, in degrees per tick. */
	public float turnDegreesPerTick(int level) {
		return turnDegrees[Math.clamp(level, 0, Bond.MAX_LEVEL)];
	}

	/** Angle off straight ahead while sidestepping (sprint key + A/D), in degrees. */
	public double sidestepAngleDegrees() {
		return sidestepAngleDegrees;
	}

	/** Ticks to go from no turn to the full turn rate. */
	public int turnEaseTicks() {
		return turnEaseTicks;
	}

	/** Chance (0.1 = 10%) per check that the horse drops one gait on its own. */
	public double gaitDropChance(int level) {
		return gaitDropChance[Math.clamp(level, 0, Bond.MAX_LEVEL)];
	}

	/** Ticks of riding without shifting gait between gait drop rolls. */
	public int gaitDropTicks(int level) {
		return gaitDropTicks[Math.clamp(level, 0, Bond.MAX_LEVEL)];
	}

	/** Whether shifting up can fail at all. */
	public boolean gaitRaiseEnabled() {
		return gaitRaise.enabled;
	}

	/** Ticks the rider can't shift up after the horse balked. */
	public int gaitRaiseCooldownTicks() {
		return (int) Math.round(gaitRaise.cooldownSeconds * 20);
	}

	/**
	 * Chance (0.06 = 6%) that asking for more speed fails, by the gait shifted up from (a gallop
	 * means a gallop push) and loyalty level.
	 */
	public double gaitRaiseFailChance(Gait from, int level) {
		return gaitRaiseFailChance[from.ordinal()][Math.clamp(level, 0, Bond.MAX_LEVEL)];
	}

	/** Whether tapping W at a gallop pushes the horse faster. */
	public boolean gallopPushEnabled() {
		return gallopPush.enabled;
	}

	/** Extra share of gallop speed per push (0.1 = 10%). */
	public double gallopPushStep() {
		return gallopPush.percentPerPush / 100.0;
	}

	/** Most extra share of gallop speed all pushes together can add (0.2 = up to 120%). */
	public double gallopPushMax() {
		return (gallopPush.maxPercent - 100) / 100.0;
	}

	/** Ticks a gallop push lasts after the last W tap. At least 1. */
	public int gallopPushHoldTicks() {
		return (int) Math.max(1, Math.round(gallopPush.holdSeconds * 20));
	}

	/** What the gallop push is multiplied by each tick while it fades (0.966 halves it in a second). */
	public double gallopPushFadePerTick() {
		// Halves every half-life: x^(20 * seconds) = 0.5.
		return Math.pow(0.5, 1 / (gallopPush.fadeHalfLifeSeconds * 20));
	}

	/** Volume (0.4 = 40%) of the straining breaths while pushing. 0 for none. */
	public float gallopPushSoundVolume() {
		return (float) (gallopPush.soundVolumePercent / 100.0);
	}
}
