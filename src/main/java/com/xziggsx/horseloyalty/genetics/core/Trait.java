package com.xziggsx.horseloyalty.genetics.core;

import com.xziggsx.horseloyalty.genetics.api.CopyRole;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Something a horse shows because of its genes, such as its coat. A trait reads a few loci
 * and decides which version wins; the loci themselves don't know. Each locus belongs to one
 * trait, so changing one trait's genes never changes another trait.
 *
 * @param <T> what the trait shows, such as a coat colour
 */
public interface Trait<T> {
	/** The id used in commands and the dev panel, such as "coat". */
	String id();

	/** The loci this trait reads. Nothing else in the genome may change what it shows. */
	List<Locus<?>> loci();

	/**
	 * What a horse with this genome shows. Usually one certain outcome; a trait whose genes
	 * compete (two different quirks) gives several, and a horse samples one at birth.
	 * A missing locus counts as its plainest pair.
	 */
	Distribution<T> express(Genome genome);

	/** Whether a shown value counts as the expected one. Equality by default. */
	default boolean matches(T actual, T expected) {
		return Objects.equals(actual, expected);
	}

	/** Whether a horse with this genome can show this value. */
	default boolean canExpress(Genome genome, T value) {
		for (T outcome : express(genome).outcomes().keySet()) {
			if (matches(outcome, value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Which copies at each of this trait's loci show on a horse with this genome that shows
	 * {@code shown}, for display. The default guesses: a copy shows if swapping it for another
	 * version could change what the horse shows. That misses copies that only matter together
	 * (black/black), so traits override it with their own rules.
	 */
	default Map<Locus<?>, CopyRole> roles(Genome genome, T shown) {
		Map<Locus<?>, CopyRole> roles = new LinkedHashMap<>();
		Distribution<T> current = express(genome);
		for (Locus<?> locus : loci()) {
			roles.put(locus, guessRole(genome, locus, current));
		}
		return roles;
	}

	private <A extends Enum<A>> CopyRole guessRole(Genome genome, Locus<A> locus, Distribution<T> current) {
		GenePair<A> pair = genome.get(locus);
		if (pair == null) {
			return CopyRole.NEITHER;
		}
		boolean first = false;
		boolean second = false;
		for (A other : locus.versions()) {
			first |= other != pair.first()
					&& !express(genome.with(locus, new GenePair<>(other, pair.second()))).outcomes().equals(current.outcomes());
			second |= other != pair.second()
					&& !express(genome.with(locus, new GenePair<>(pair.first(), other))).outcomes().equals(current.outcomes());
		}
		return new CopyRole(first, second);
	}

	/** Casts an untyped value to this trait's type, for code that holds traits of any type. */
	@SuppressWarnings("unchecked")
	default T cast(Object value) {
		return (T) value;
	}
}
