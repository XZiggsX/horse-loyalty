package com.xziggsx.horseloyalty.core.feature;

import com.xziggsx.horseloyalty.core.config.SettingsSection;
import java.util.Set;

/**
 * One major part of the mod that can be switched off on its own. Every feature has one class
 * like this in its own package; it only wires the feature up and holds no game logic.
 */
public interface Feature {
	/** The id used in the config's "features" block and the command, such as "riding". */
	String id();

	/** Ids of features this one can't run without. Only hunger has one ("loyalty"). */
	default Set<String> requires() {
		return Set.of();
	}

	/**
	 * Always runs, on or off: attachments, data components and packet types. Saved data must
	 * stay registered, or turning a feature off would wipe it from every horse on the next save.
	 */
	void registerData();

	/** Always runs once: hooks the feature up through the context, which gates everything by the switch. */
	void wire(FeatureContext context);

	/** A fresh default instance of this feature's config section, or null if it has none. */
	default SettingsSection settings() {
		return null;
	}
}
