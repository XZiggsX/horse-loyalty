package com.xziggsx.horseloyalty.genetics.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A horse's genes: a {@link GenePair} for each locus it has. Immutable; {@link #with} returns
 * a copy. Any locus can be missing (say, a save from before that locus was added), see
 * {@link GenomeFactory#fillMissing}.
 */
public final class Genome {
	public static final Genome EMPTY = new Genome(Map.of());

	private final Map<Locus<?>, GenePair<?>> pairs;

	private Genome(Map<Locus<?>, GenePair<?>> pairs) {
		this.pairs = pairs;
	}

	/** The pair at this locus, or null if the genome doesn't have it. */
	@SuppressWarnings("unchecked")
	public <A extends Enum<A>> GenePair<A> get(Locus<A> locus) {
		return (GenePair<A>) pairs.get(locus);
	}

	/** The pair at this locus, or the fallback if the genome doesn't have it. */
	public <A extends Enum<A>> GenePair<A> getOr(Locus<A> locus, GenePair<A> fallback) {
		GenePair<A> pair = get(locus);
		return pair != null ? pair : fallback;
	}

	/** Whether the genome has this locus. */
	public boolean has(Locus<?> locus) {
		return pairs.containsKey(locus);
	}

	/** A copy with this locus set to the pair. */
	public <A extends Enum<A>> Genome with(Locus<A> locus, GenePair<A> pair) {
		return withUnchecked(locus, pair);
	}

	/** {@link #with} for code that holds loci and pairs of any type. The pair must belong to the locus. */
	Genome withUnchecked(Locus<?> locus, GenePair<?> pair) {
		Map<Locus<?>, GenePair<?>> copy = new LinkedHashMap<>();
		// Kept in Loci.ALL order, so saves and the short form read the same for every horse.
		for (Locus<?> each : Loci.ALL) {
			GenePair<?> value = each == locus ? pair : pairs.get(each);
			if (value != null) {
				copy.put(each, value);
			}
		}
		return new Genome(Collections.unmodifiableMap(copy));
	}

	/** The pairs by locus, in {@link Loci#ALL} order. */
	public Map<Locus<?>, GenePair<?>> pairs() {
		return pairs;
	}

	/** For saving: locus id to "first/second" version names. */
	public Map<String, String> toStrings() {
		Map<String, String> strings = new LinkedHashMap<>();
		pairs.forEach((locus, pair) -> strings.put(locus.id(), pair.toString()));
		return strings;
	}

	/** Reads {@link #toStrings()}. Unknown loci, unknown versions and broken entries are skipped. */
	public static Genome fromStrings(Map<String, String> strings) {
		Genome genome = EMPTY;
		for (Map.Entry<String, String> entry : strings.entrySet()) {
			Locus<?> locus = Loci.byId(entry.getKey());
			if (locus != null && entry.getValue() != null) {
				genome = parseInto(genome, locus, entry.getValue());
			}
		}
		return genome;
	}

	private static <A extends Enum<A>> Genome parseInto(Genome genome, Locus<A> locus, String value) {
		String[] parts = value.split("/");
		if (parts.length != 2) {
			return genome;
		}
		A first = locus.parse(parts[0]);
		A second = locus.parse(parts[1]);
		return first == null || second == null ? genome : genome.with(locus, new GenePair<>(first, second));
	}

	/** Short form for chat and the dev panel, such as "red_black black/red, agouti bay/bay". */
	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");
		pairs.forEach((locus, pair) -> joiner.add(locus.id() + " " + pair));
		return joiner.toString();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Genome genome && pairs.equals(genome.pairs);
	}

	@Override
	public int hashCode() {
		return pairs.hashCode();
	}
}
