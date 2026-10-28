package com.xziggsx.horseloyalty.summoning;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;

/** The summoning section of the config. Field names are the JSON keys. */
@SuppressWarnings("FieldMayBeFinal")
public final class SummoningSettings implements SettingsSection {
	@Range(min = 0, max = 5)
	@Doc("Loyalty level a player needs with a horse to bind a horn to it and to call it.")
	private int minLevel = 2;

	@Range(min = 0, max = 5)
	@Doc("Loyalty level at which a horse answers from another dimension.")
	private int crossDimensionLevel = 5;

	@Range(min = 1, max = 3600)
	@Doc("A called horse gives up after this many seconds if it still hasn't reached the player.")
	private double callSeconds = 30;

	@Range(min = 0, max = 128)
	@Doc("A horse this close just runs over; one further away is moved to open ground behind the player first.")
	private double runOverDistance = 24;

	/** The section in use. */
	public static SummoningSettings get() {
		return ConfigFile.section(SummoningSettings.class);
	}

	@Override
	public String key() {
		return "summoning";
	}

	/** Loyalty level a player needs with a horse to bind a horn to it and to call it. */
	public int minLevel() {
		return minLevel;
	}

	/** Loyalty level at which a horse answers from another dimension. */
	public int crossDimensionLevel() {
		return crossDimensionLevel;
	}

	/** A called horse gives up after this many ticks. */
	public int callTicks() {
		return (int) Math.round(callSeconds * 20);
	}

	/** Blocks within which a called horse just runs over. */
	public double runOverDistance() {
		return runOverDistance;
	}
}
