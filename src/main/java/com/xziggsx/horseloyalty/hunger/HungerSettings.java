package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;

/** The hunger section of the config. Field names are the JSON keys. */
@SuppressWarnings("FieldMayBeFinal")
public final class HungerSettings implements SettingsSection {
	@Range(min = 1, max = 10_000_000)
	@Doc("A tamed horse that hasn't been fed or left to graze (unridden, on a grazing block) for this long gets hungry and shows dark particles.")
	private double secondsUntilHungry = 1200;

	@Range(min = 1, max = 10_000_000)
	@Doc("While hungry, it loses loyaltyPointsLost points with every rider this often. It never drops below level 1 or becomes untamed.")
	private double loyaltyLossEverySeconds = 120;

	@Range(min = 0, max = 1000)
	@Doc("Points every rider loses at each hunger loss.")
	private int loyaltyPointsLost = 1;

	/** The section in use. */
	public static HungerSettings get() {
		return ConfigFile.section(HungerSettings.class);
	}

	@Override
	public String key() {
		return "hunger";
	}

	/** Ticks without eating or grazing before a horse is hungry. */
	public long ticksUntilHungry() {
		return Math.round(secondsUntilHungry * 20);
	}

	/** Ticks between loyalty losses while a horse is hungry. */
	public long loyaltyLossIntervalTicks() {
		return Math.round(loyaltyLossEverySeconds * 20);
	}

	/** Points every rider loses at each hunger loss. */
	public int loyaltyPointsLost() {
		return loyaltyPointsLost;
	}
}
