package com.xziggsx.horseloyalty.genetics.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The exact odds of what a foal of two horses will show. Works out every pair the foal can get
 * at the trait's loci (and only those, so it stays small), with mutation, and adds up what
 * each combination shows. Matches {@link Inheritance#cross} on average.
 */
public final class CrossPredictor {
	private CrossPredictor() {
	}

	/**
	 * The chance of each value of this trait in a foal of a and b.
	 *
	 * @param mutationChance chance (0.01 = 1%) that a passed copy mutates, as in {@link Inheritance#cross}
	 */
	public static <T> Distribution<T> predict(Genome a, Genome b, Trait<T> trait, double mutationChance) {
		List<Locus<?>> loci = trait.loci();
		List<Map<GenePair<?>, Double>> pairsByLocus = new ArrayList<>();
		for (Locus<?> locus : loci) {
			pairsByLocus.add(new LinkedHashMap<>(childPairs(a, b, locus, mutationChance)));
		}
		Map<T, Double> totals = new LinkedHashMap<>();
		enumerate(trait, loci, pairsByLocus, 0, Genome.EMPTY, 1.0, totals);
		return Distribution.of(totals);
	}

	/** {@link #predict} for each of these traits, in their order. */
	public static Map<Trait<?>, Distribution<?>> predictAll(Genome a, Genome b, List<Trait<?>> traits, double mutationChance) {
		Map<Trait<?>, Distribution<?>> result = new LinkedHashMap<>();
		for (Trait<?> trait : traits) {
			result.put(trait, predict(a, b, trait, mutationChance));
		}
		return result;
	}

	private static <T> void enumerate(Trait<T> trait, List<Locus<?>> loci, List<Map<GenePair<?>, Double>> pairsByLocus,
			int index, Genome genome, double chance, Map<T, Double> totals) {
		if (index == loci.size()) {
			trait.express(genome).outcomes().forEach((outcome, share) -> totals.merge(outcome, chance * share, Double::sum));
			return;
		}
		Locus<?> locus = loci.get(index);
		for (Map.Entry<GenePair<?>, Double> entry : pairsByLocus.get(index).entrySet()) {
			enumerate(trait, loci, pairsByLocus, index + 1, genome.withUnchecked(locus, entry.getKey()),
					chance * entry.getValue(), totals);
		}
	}

	/** Every pair a foal can get at this locus, with its chance. */
	private static <A extends Enum<A>> Map<GenePair<A>, Double> childPairs(Genome a, Genome b, Locus<A> locus,
			double mutationChance) {
		Map<A, Double> fromA = passedCopy(a.get(locus), locus, mutationChance);
		Map<A, Double> fromB = passedCopy(b.get(locus), locus, mutationChance);
		Map<GenePair<A>, Double> pairs = new LinkedHashMap<>();
		fromA.forEach((first, chanceA) -> fromB.forEach((second, chanceB) ->
				pairs.merge(new GenePair<>(first, second), chanceA * chanceB, Double::sum)));
		return pairs;
	}

	/** The chance of each version a parent passes on: either copy, then maybe a mutation. A missing locus passes any version. */
	private static <A extends Enum<A>> Map<A, Double> passedCopy(GenePair<A> pair, Locus<A> locus, double mutationChance) {
		Map<A, Double> chances = new LinkedHashMap<>();
		int count = locus.versions().size();
		double mutation = pair == null ? 1 : Math.clamp(mutationChance, 0, 1);
		for (A version : locus.versions()) {
			chances.put(version, mutation / count);
		}
		if (pair != null) {
			chances.merge(pair.first(), (1 - mutation) / 2, Double::sum);
			chances.merge(pair.second(), (1 - mutation) / 2, Double::sum);
		}
		chances.values().removeIf(chance -> chance <= 0);
		return chances;
	}
}
