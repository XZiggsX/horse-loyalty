package com.xziggsx.horseloyalty.foals;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;

/** The foals section of the config. Field names are the JSON keys. */
@SuppressWarnings("FieldMayBeFinal")
public final class FoalSettings implements SettingsSection {
	@Range(min = 0, max = 100)
	@Doc("A foal bred by a player is born tamed to them, with this share of the average of their points with the two parents.")
	private double parentLoyaltyPercent = 25;

	@Range(min = 2, max = 64)
	@Doc("The foal picks one parent at random to follow once more than this many blocks away.")
	private double followParentDistance = 5;

	@Range(min = 2, max = 64)
	@Doc("It gives up following its parent beyond this many blocks.")
	private double followParentMaxDistance = 24;

	@Range(min = 2, max = 64)
	@Doc("Without that parent near, it follows its breeder from this many blocks.")
	private double followPlayerDistance = 4;

	@Range(min = 2, max = 64)
	@Doc("It gives up following its breeder beyond this many blocks.")
	private double followPlayerMaxDistance = 16;

	@Range(min = 0, max = 100)
	@Doc("Share of its random strolls a foal skips.")
	private double strollSkipPercent = 50;

	@Range(min = 1, max = 64)
	@Doc("A foal never strolls further than this from its parent or breeder. Foals that spawn in the wild act as in vanilla.")
	private double strollMaxDistance = 6;

	/** The section in use. */
	public static FoalSettings get() {
		return ConfigFile.section(FoalSettings.class);
	}

	@Override
	public String key() {
		return "foals";
	}

	@Override
	public void resolve() {
		followParentMaxDistance = Math.max(followParentMaxDistance, followParentDistance);
		followPlayerMaxDistance = Math.max(followPlayerMaxDistance, followPlayerDistance);
	}

	/** Share (0.25 = 25%) of the breeder's average points with the parents that a foal starts with. */
	public double parentLoyaltyShare() {
		return parentLoyaltyPercent / 100.0;
	}

	/** Blocks from its parent before a foal starts following. */
	public double followParentDistance() {
		return followParentDistance;
	}

	/** Beyond this many blocks a foal gives up following its parent. */
	public double followParentMaxDistance() {
		return followParentMaxDistance;
	}

	/** Blocks from its breeder before a foal starts following them. */
	public double followPlayerDistance() {
		return followPlayerDistance;
	}

	/** Beyond this many blocks a foal gives up following its breeder. */
	public double followPlayerMaxDistance() {
		return followPlayerMaxDistance;
	}

	/** Chance (0.5 = 50%) that a foal skips a random stroll. */
	public double strollSkipChance() {
		return strollSkipPercent / 100.0;
	}

	/** Foals don't stroll further than this from their parent or breeder. */
	public double strollMaxDistance() {
		return strollMaxDistance;
	}
}
