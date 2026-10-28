package com.xziggsx.horseloyalty.genetics.core;

import java.util.List;
import java.util.Locale;

/**
 * One place in the genome, holding a pair of copies (see {@link GenePair}). It only lists the
 * possible versions; which one wins is up to the {@link Trait}s that read it.
 *
 * @param <A> the enum of versions this locus can hold
 */
public final class Locus<A extends Enum<A>> {
	private final String id;
	private final List<A> versions;

	public Locus(String id, Class<A> type) {
		this.id = id;
		this.versions = List.of(type.getEnumConstants());
	}

	/** The id used in saves and commands, such as "red_black". */
	public String id() {
		return id;
	}

	/** Every version, in declaration order. */
	public List<A> versions() {
		return versions;
	}

	/** A uniformly random version. */
	public A random(Dice dice) {
		return versions.get(dice.nextInt(versions.size()));
	}

	/** A pair of two independently random versions. */
	public GenePair<A> randomPair(Dice dice) {
		return new GenePair<>(random(dice), random(dice));
	}

	/** The version with this name (any case), or null if there is none. */
	public A parse(String name) {
		String upper = name.trim().toUpperCase(Locale.ROOT);
		for (A version : versions) {
			if (version.name().equals(upper)) {
				return version;
			}
		}
		return null;
	}

	/** The name a version is saved and typed as, such as "seal_brown". */
	public static String name(Enum<?> version) {
		return version.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public String toString() {
		return id;
	}
}
