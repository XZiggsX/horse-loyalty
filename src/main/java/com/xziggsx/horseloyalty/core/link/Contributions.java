package com.xziggsx.horseloyalty.core.link;

import com.xziggsx.horseloyalty.core.feature.Features;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A list that any feature can add to, such as the ride modifiers or the horse info providers.
 * Like a {@link LinkKey}, but with many providers: each item belongs to the feature that added it,
 * and only items whose feature is on are handed out. Features add items through their
 * {@code FeatureContext}; the list itself lives in the reading feature's {@code api} package.
 *
 * @param <T> what the list holds
 */
public final class Contributions<T> {
	/** An item and the feature it belongs to. */
	private record Entry<T>(String featureId, T item) {
	}

	private final String id;
	private final List<Entry<T>> entries = new CopyOnWriteArrayList<>();

	/** @param id a unique name such as "riding:ride_modifiers", for logs */
	public Contributions(String id) {
		this.id = id;
	}

	/** The list's name. */
	public String id() {
		return id;
	}

	/** Adds an item for a feature. Called through {@code FeatureContext.contribute}. */
	public void add(String featureId, T item) {
		entries.add(new Entry<>(featureId, item));
	}

	/** The items whose feature is on now, in the order they were added. */
	public List<T> active() {
		List<T> result = new ArrayList<>(entries.size());
		for (Entry<T> entry : entries) {
			if (Features.isOn(entry.featureId())) {
				result.add(entry.item());
			}
		}
		return result;
	}
}
