package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import java.util.ArrayList;
import java.util.List;

/**
 * The loyalty section of the config: how fast loyalty builds, and the points each level needs.
 * Field names are the JSON keys, so renaming one breaks existing config files.
 */
@SuppressWarnings("FieldMayBeFinal")
public final class LoyaltySettings implements SettingsSection {
	/** Points per level 1 to 5 by default. Level 1 is just tamed. */
	private static final List<Integer> DEFAULT_POINTS = List.of(0, 10, 40, 100, 200);

	@Range(min = 0, max = 1000)
	@Doc("Points for each feed, but only once per feedCooldownSeconds per player.")
	private int pointsPerFeed = 2;

	@Range(min = 0, max = 86400)
	@Doc("Seconds before the same player's next feed counts again.")
	private double feedCooldownSeconds = 30;

	@Range(min = 0.05, max = 86400)
	@Doc("Riding a moving horse gives 1 point per this many seconds.")
	private double ridingSecondsPerPoint = 60;

	@Doc("Total points to reach each level, 1 (Unruly) to 5 (Partners). Each level needs at least one point more than the one below.")
	private List<Integer> pointsNeeded = new ArrayList<>(DEFAULT_POINTS);

	private transient int[] pointsByLevel;

	/** The section in use. */
	public static LoyaltySettings get() {
		return ConfigFile.section(LoyaltySettings.class);
	}

	@Override
	public String key() {
		return "loyalty";
	}

	@Override
	public void resolve() {
		pointsByLevel = new int[Bond.MAX_LEVEL + 1];
		for (int level = 2; level <= Bond.MAX_LEVEL; level++) {
			Integer points = level <= pointsNeeded.size() ? pointsNeeded.get(level - 1) : null;
			int value = points != null && points > 0 ? points : DEFAULT_POINTS.get(level - 1);
			pointsByLevel[level] = Math.max(value, pointsByLevel[level - 1] + 1);
		}
	}

	/** Loyalty points for each counted feed. */
	public int pointsPerFeed() {
		return pointsPerFeed;
	}

	/** Minimum ticks between two feeds that count, per player. */
	public long feedCooldownTicks() {
		return Math.round(feedCooldownSeconds * 20);
	}

	/** Ticks of riding a moving horse per loyalty point. Always at least 1. */
	public int ridingTicksPerPoint() {
		return (int) Math.max(1, Math.round(ridingSecondsPerPoint * 20));
	}

	/** Total points needed to reach a level. Levels 0 and 1 need none. */
	public int pointsNeeded(int level) {
		return pointsByLevel[Math.clamp(level, 0, Bond.MAX_LEVEL)];
	}
}
