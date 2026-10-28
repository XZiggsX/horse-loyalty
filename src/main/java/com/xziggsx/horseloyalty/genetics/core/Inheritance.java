package com.xziggsx.horseloyalty.genetics.core;

/** How a foal's genome comes from its parents. */
public final class Inheritance {
	private Inheritance() {
	}

	/**
	 * A foal's genome: at each locus, each parent passes one of its two copies at random, and
	 * each passed copy may mutate into a uniformly random version (which can be the same one).
	 * A locus that only one parent has is left out; {@link GenomeFactory#fillMissing} can fill it.
	 *
	 * @param mutationChance chance (0.01 = 1%) that a passed copy mutates
	 */
	public static Genome cross(Genome a, Genome b, double mutationChance, Dice dice) {
		Genome child = Genome.EMPTY;
		for (Locus<?> locus : Loci.ALL) {
			child = crossLocus(child, a, b, locus, mutationChance, dice);
		}
		return child;
	}

	private static <A extends Enum<A>> Genome crossLocus(Genome child, Genome a, Genome b, Locus<A> locus,
			double mutationChance, Dice dice) {
		GenePair<A> fromA = a.get(locus);
		GenePair<A> fromB = b.get(locus);
		if (fromA == null || fromB == null) {
			return child;
		}
		A first = mutate(fromA.pickOne(dice), locus, mutationChance, dice);
		A second = mutate(fromB.pickOne(dice), locus, mutationChance, dice);
		return child.with(locus, new GenePair<>(first, second));
	}

	private static <A extends Enum<A>> A mutate(A version, Locus<A> locus, double mutationChance, Dice dice) {
		return mutationChance > 0 && dice.nextDouble() < mutationChance ? locus.random(dice) : version;
	}
}
