package com.xziggsx.horseloyalty.core.feature;

import java.util.Locale;

/**
 * Whether a feature is switched on for the whole server. Set in the config's "features" block or
 * with {@code /hl feature}; a client always follows the server's switches.
 */
public enum FeatureSwitch {
	ON,
	OFF;

	/** The word used in the config and the command: "on" or "off". */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The switch for "on" or "off" (any case), or null for anything else. */
	public static FeatureSwitch byId(String id) {
		for (FeatureSwitch value : values()) {
			if (value.id().equalsIgnoreCase(id)) {
				return value;
			}
		}
		return null;
	}
}
