package com.xziggsx.horseloyalty.core.feature;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which features exist and which are on. Each side reads its own source: the server its config,
 * a client the switches the server sent it (or its own config while not connected to a server
 * with the mod). A feature that is switched on but needs one that is off counts as off, and the
 * log says why, once.
 */
public final class Features {
	private static final Map<String, Feature> FEATURES = new LinkedHashMap<>();
	private static final Map<String, GatedFeatureContext> CONTEXTS = new HashMap<>();
	/** Switches from this side's own config. Missing ids are on. */
	private static Map<String, FeatureSwitch> localSwitches = Map.of();
	/** Client: the switches the server sent, or null while not connected to a server with the mod. */
	private static Map<String, FeatureSwitch> remoteSwitches;
	/** Whether each feature is on, with requirements applied. Rebuilt whenever a switch changes. */
	private static volatile Map<String, Boolean> resolved = Map.of();
	/** Features already logged as off because of a requirement, so the log says it once. */
	private static final Set<String> LOGGED_OFF = new HashSet<>();

	private Features() {
	}

	/** Adds a feature. Call this for every feature before {@link #wireAll}. */
	public static synchronized void register(Feature feature) {
		if (FEATURES.putIfAbsent(feature.id(), feature) != null) {
			throw new IllegalArgumentException("Feature " + feature.id() + " is registered twice");
		}
		resolve();
	}

	/** Called once after the config loads: registerData() for every feature, then wire() for every feature. */
	public static void wireAll() {
		for (Feature feature : FEATURES.values()) {
			feature.registerData();
		}
		for (Feature feature : FEATURES.values()) {
			feature.wire(context(feature.id()));
		}
	}

	/** Every registered feature, in registration order. */
	public static Collection<Feature> all() {
		return Collections.unmodifiableCollection(FEATURES.values());
	}

	/** The registered feature with this id, if any. */
	public static Optional<Feature> byId(String id) {
		return Optional.ofNullable(FEATURES.get(id));
	}

	/** The context a feature hooks in with. Everything added through it is gated by that feature's switch. */
	public static synchronized FeatureContext context(String id) {
		return CONTEXTS.computeIfAbsent(id, GatedFeatureContext::new);
	}

	/** Whether a feature is on in this world, including its requirements. Works on both sides. Unknown ids are off. */
	public static boolean isOn(String id) {
		return resolved.getOrDefault(id, false);
	}

	/** The feature's own switch on this side, ignoring its requirements. */
	public static FeatureSwitch switchOf(String id) {
		Map<String, FeatureSwitch> switches = remoteSwitches != null ? remoteSwitches : localSwitches;
		return switches.getOrDefault(id, FeatureSwitch.ON);
	}

	/** A required feature that keeps this one off although its own switch is on, if there is one. */
	public static Optional<String> offBecause(String id) {
		Feature feature = FEATURES.get(id);
		if (feature == null || switchOf(id) == FeatureSwitch.OFF) {
			return Optional.empty();
		}
		return feature.requires().stream().filter(required -> !isOn(required)).findFirst();
	}

	/** This side's switches from its own config. */
	public static synchronized void useLocal(Map<String, FeatureSwitch> switches) {
		localSwitches = Map.copyOf(switches);
		resolve();
	}

	/** Client: follow the server's switches from now on, or this side's own config again with null (on disconnecting). */
	public static synchronized void useRemote(Map<String, FeatureSwitch> switches) {
		remoteSwitches = switches == null ? null : Map.copyOf(switches);
		resolve();
	}

	/** This side's own switches, every registered feature included. */
	public static synchronized Map<String, FeatureSwitch> localSwitches() {
		Map<String, FeatureSwitch> switches = new LinkedHashMap<>();
		for (String id : FEATURES.keySet()) {
			switches.put(id, localSwitches.getOrDefault(id, FeatureSwitch.ON));
		}
		return switches;
	}

	/** Works out which features are on, requirements included, and logs any that a requirement keeps off. */
	private static void resolve() {
		Map<String, Boolean> result = new HashMap<>();
		for (String id : FEATURES.keySet()) {
			resolveOne(id, result, new HashSet<>());
		}
		resolved = Map.copyOf(result);
		for (Feature feature : FEATURES.values()) {
			String id = feature.id();
			if (switchOf(id) == FeatureSwitch.ON && !result.get(id)) {
				if (LOGGED_OFF.add(id)) {
					HorseLoyaltyMod.LOGGER.info("Feature {} is switched on but stays off: it needs {}", id, feature.requires());
				}
			} else {
				LOGGED_OFF.remove(id);
			}
		}
	}

	private static boolean resolveOne(String id, Map<String, Boolean> result, Set<String> visiting) {
		Boolean known = result.get(id);
		if (known != null) {
			return known;
		}
		Feature feature = FEATURES.get(id);
		// Unknown features and requirement loops count as off.
		if (feature == null || !visiting.add(id)) {
			return false;
		}
		boolean on = switchOf(id) == FeatureSwitch.ON;
		for (String required : feature.requires()) {
			on &= resolveOne(required, result, visiting);
		}
		result.put(id, on);
		return on;
	}
}
