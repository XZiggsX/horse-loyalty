package com.xziggsx.horseloyalty.genetics;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.Doc;
import com.xziggsx.horseloyalty.core.config.Range;
import com.xziggsx.horseloyalty.core.config.SettingsSection;

/** The genetics section of the config. Field names are the JSON keys. */
@SuppressWarnings("FieldMayBeFinal")
public final class GeneticsSettings implements SettingsSection {
	@Range(min = 0, max = 100)
	@Doc("Chance that a gene copy passed to a foal becomes a random version. A foal gets one random copy of each gene from each parent, so foals can show colours their parents only carry.")
	private double mutationPercent = 1;

	/** The section in use. */
	public static GeneticsSettings get() {
		return ConfigFile.section(GeneticsSettings.class);
	}

	@Override
	public String key() {
		return "genetics";
	}

	/** Chance (0.01 = 1%) of a mutation per copy. */
	public double mutationChance() {
		return mutationPercent / 100.0;
	}
}
