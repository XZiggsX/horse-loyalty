package com.xziggsx.horseloyalty.core.config;

import com.google.gson.JsonObject;

/**
 * One feature's section of the config file. A plain class whose fields are the JSON keys, with
 * their defaults as field values; Gson fills them in. Limits are declared with {@link Range} and
 * explained with {@link Doc}. The {@link SettingsResolver} clamps and fills in defaults, then calls
 * {@link #resolve()} for anything that depends on more than one field.
 * <p>
 * Each section has its own {@link #version()}, so it can migrate its own layout without core
 * knowing it.
 */
public interface SettingsSection {
	/** The key of this section in the config file, such as "riding". */
	String key();

	/** Bump when the layout changes in a way {@link #migrate} has to handle. */
	default int version() {
		return 1;
	}

	/** Brings this section's JSON up to {@link #version()} from an older one, in place. */
	default void migrate(JsonObject section, int fromVersion) {
	}

	/** Works out derived values and rules that span several fields. Runs after clamping. */
	default void resolve() {
	}
}
