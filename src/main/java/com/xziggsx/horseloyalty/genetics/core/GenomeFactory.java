package com.xziggsx.horseloyalty.genetics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Makes genomes to fit a horse that already looks a certain way: wild herd founders, and
 * horses from before genetics was on. The genes behind a look are random, so two bay horses
 * can carry different hidden copies.
 */
public final class GenomeFactory {
	/** Random tries before falling back to listing every combination. */
	private static final int TRIES = 64;

	private GenomeFactory() {
	}

	/**
	 * A random, complete genome that shows the wanted value of each trait in the map.
	 * Values a trait can't show at all are skipped.
	 *
	 * @param look the wanted value by trait, such as coat to {@code Variant.BROWN}
	 */
	public static Genome forLook(Map<Trait<?>, Object> look, Dice dice) {
		Genome genome = Genome.EMPTY;
		for (Map.Entry<Trait<?>, Object> entry : look.entrySet()) {
			genome = refitUnchecked(genome, entry.getKey(), entry.getValue(), dice);
		}
		return fillMissing(genome, dice);
	}

	/** A copy with a random pair at every locus the genome doesn't have yet. */
	public static Genome fillMissing(Genome genome, Dice dice) {
		Genome result = genome;
		for (Locus<?> locus : Loci.ALL) {
			if (!result.has(locus)) {
				result = result.withUnchecked(locus, locus.randomPair(dice));
			}
		}
		return result;
	}

	/**
	 * A copy that shows this value of the trait, with the trait's loci re-rolled if they have
	 * to change. Unchanged if it already can show it (and has all those loci), or if no genes
	 * can. Tries random pairs first, then picks from every combination that fits.
	 */
	public static <T> Genome refit(Genome genome, Trait<T> trait, T value, Dice dice) {
		if (hasLoci(genome, trait) && trait.canExpress(genome, value)) {
			return genome;
		}
		for (int i = 0; i < TRIES; i++) {
			Genome candidate = genome;
			for (Locus<?> locus : trait.loci()) {
				candidate = candidate.withUnchecked(locus, locus.randomPair(dice));
			}
			if (trait.canExpress(candidate, value)) {
				return candidate;
			}
		}
		// Unlucky or rare: list every combination at the trait's loci, then pick a random fitting one.
		List<Genome> fitting = new ArrayList<>();
		collectFitting(trait, value, trait.loci(), 0, genome, fitting);
		return fitting.isEmpty() ? genome : fitting.get(dice.nextInt(fitting.size()));
	}

	private static <T> Genome refitUnchecked(Genome genome, Trait<T> trait, Object value, Dice dice) {
		return refit(genome, trait, trait.cast(value), dice);
	}

	private static boolean hasLoci(Genome genome, Trait<?> trait) {
		for (Locus<?> locus : trait.loci()) {
			if (!genome.has(locus)) {
				return false;
			}
		}
		return true;
	}

	private static <T> void collectFitting(Trait<T> trait, T value, List<Locus<?>> loci, int index, Genome genome,
			List<Genome> fitting) {
		if (index == loci.size()) {
			if (trait.canExpress(genome, value)) {
				fitting.add(genome);
			}
			return;
		}
		Locus<?> locus = loci.get(index);
		for (GenePair<?> pair : allPairs(locus)) {
			collectFitting(trait, value, loci, index + 1, genome.withUnchecked(locus, pair), fitting);
		}
	}

	private static <A extends Enum<A>> List<GenePair<A>> allPairs(Locus<A> locus) {
		List<GenePair<A>> pairs = new ArrayList<>();
		for (A first : locus.versions()) {
			for (A second : locus.versions()) {
				pairs.add(new GenePair<>(first, second));
			}
		}
		return pairs;
	}
}
