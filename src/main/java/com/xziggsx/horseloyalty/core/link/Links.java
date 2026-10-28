package com.xziggsx.horseloyalty.core.link;

import com.xziggsx.horseloyalty.core.feature.Features;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The link registry: who provides each {@link LinkKey}. Features provide links through their
 * {@code FeatureContext}, so each provider is tied to its feature's switch.
 */
public final class Links {
	/** A provider and the feature it belongs to. */
	private record Provider(String featureId, Object implementation) {
	}

	private static final Map<LinkKey<?>, Provider> PROVIDERS = new ConcurrentHashMap<>();

	private Links() {
	}

	/** The provider if its feature is on, otherwise the key's neutral stand-in. Never null. */
	public static <T> T get(LinkKey<T> key) {
		Provider provider = PROVIDERS.get(key);
		if (provider == null || !Features.isOn(provider.featureId())) {
			return key.neutral();
		}
		return key.type().cast(provider.implementation());
	}

	/**
	 * Makes a feature the provider of a key, replacing any earlier one (the client side of a feature
	 * may replace its common provider). Called through {@code FeatureContext.provide}.
	 */
	public static <T> void provide(String featureId, LinkKey<T> key, T implementation) {
		PROVIDERS.put(key, new Provider(featureId, key.type().cast(implementation)));
	}
}
