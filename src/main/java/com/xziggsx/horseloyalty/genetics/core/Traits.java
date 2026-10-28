package com.xziggsx.horseloyalty.genetics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Every registered trait. The traits themselves live in the trait layer and register there. */
public final class Traits {
	private static final List<Trait<?>> REGISTERED = new ArrayList<>();
	/** Every registered trait, in registration order. */
	public static final List<Trait<?>> ALL = Collections.unmodifiableList(REGISTERED);

	private Traits() {
	}

	/** Adds a trait to {@link #ALL}. Its id must be new. */
	public static synchronized <T extends Trait<?>> T register(T trait) {
		if (byId(trait.id()) != null) {
			throw new IllegalArgumentException("Duplicate trait " + trait.id());
		}
		REGISTERED.add(trait);
		return trait;
	}

	/** The trait with this id, or null. */
	public static Trait<?> byId(String id) {
		for (Trait<?> trait : REGISTERED) {
			if (trait.id().equals(id)) {
				return trait;
			}
		}
		return null;
	}
}
