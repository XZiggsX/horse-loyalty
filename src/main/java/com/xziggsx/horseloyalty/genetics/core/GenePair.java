package com.xziggsx.horseloyalty.genetics.core;

/**
 * The two copies a horse has at one locus, one from each parent. The order means nothing to
 * the traits: A/B and B/A show the same.
 */
public record GenePair<A extends Enum<A>>(A first, A second) {
	/** Both copies the same version. */
	public static <A extends Enum<A>> GenePair<A> of(A version) {
		return new GenePair<>(version, version);
	}

	/** One of the two copies, 50/50, as passed on to a foal. */
	public A pickOne(Dice dice) {
		return dice.nextInt(2) == 0 ? first : second;
	}

	/** How many of the two copies (0, 1 or 2) are this version. */
	public int count(A version) {
		return (first == version ? 1 : 0) + (second == version ? 1 : 0);
	}

	/** Whether at least one copy is this version. */
	public boolean has(A version) {
		return first == version || second == version;
	}

	/** Short form, such as "black/red". Also the saved form. */
	@Override
	public String toString() {
		return Locus.name(first) + "/" + Locus.name(second);
	}
}
