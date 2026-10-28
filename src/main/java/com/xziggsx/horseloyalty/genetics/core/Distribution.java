package com.xziggsx.horseloyalty.genetics.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outcomes with their chances, which add up to 1. Immutable.
 *
 * @param <T> what the outcomes are, such as a coat
 */
public final class Distribution<T> {
	private final Map<T, Double> outcomes;

	private Distribution(Map<T, Double> outcomes) {
		this.outcomes = outcomes;
	}

	/** One certain outcome. */
	public static <T> Distribution<T> single(T outcome) {
		return new Distribution<>(Map.of(outcome, 1.0));
	}

	/** Outcomes with weights, scaled so they add up to 1. Outcomes without weight are left out. */
	public static <T> Distribution<T> of(Map<T, Double> weights) {
		double total = 0;
		for (double weight : weights.values()) {
			total += Math.max(0, weight);
		}
		Map<T, Double> scaled = new LinkedHashMap<>();
		for (Map.Entry<T, Double> entry : weights.entrySet()) {
			if (entry.getValue() > 0) {
				scaled.merge(entry.getKey(), entry.getValue() / total, Double::sum);
			}
		}
		return new Distribution<>(Collections.unmodifiableMap(scaled));
	}

	/** The chance (0.25 = 25%) of this outcome; 0 if it can't happen. */
	public double chance(T outcome) {
		return outcomes.getOrDefault(outcome, 0.0);
	}

	/** Every possible outcome and its chance. */
	public Map<T, Double> outcomes() {
		return outcomes;
	}

	/** Picks an outcome by its chance. Null only for an empty distribution. */
	public T sample(Dice dice) {
		double pick = dice.nextDouble();
		T last = null;
		for (Map.Entry<T, Double> entry : outcomes.entrySet()) {
			last = entry.getKey();
			pick -= entry.getValue();
			if (pick < 0) {
				return last;
			}
		}
		// Rounding can leave a sliver past the last outcome.
		return last;
	}

	@Override
	public String toString() {
		return outcomes.toString();
	}
}
