package com.xziggsx.horseloyalty.genetics.trait;

import com.xziggsx.horseloyalty.genetics.api.CopyRole;
import com.xziggsx.horseloyalty.genetics.core.Distribution;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Loci.Temperament;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.personality.api.Personality;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A horse's {@link Personality}, from the temperament locus. Any quirk beats steady. A horse
 * with two different quirks can show either, 50/50; which one is picked once at birth.
 */
public final class TemperamentTrait implements Trait<Personality> {
	private static final List<Locus<?>> LOCI = List.of(Loci.TEMPERAMENT);

	@Override
	public String id() {
		return "temperament";
	}

	@Override
	public List<Locus<?>> loci() {
		return LOCI;
	}

	@Override
	public Distribution<Personality> express(Genome genome) {
		GenePair<Temperament> pair = genome.getOr(Loci.TEMPERAMENT, GenePair.of(Temperament.STEADY));
		Temperament first = pair.first();
		Temperament second = pair.second();
		if (first == second || second == Temperament.STEADY) {
			return Distribution.single(personality(first));
		}
		if (first == Temperament.STEADY) {
			return Distribution.single(personality(second));
		}
		Map<Personality, Double> split = new LinkedHashMap<>();
		split.put(personality(first), 0.5);
		split.put(personality(second), 0.5);
		return Distribution.of(split);
	}

	/**
	 * The copies of the personality the horse shows. With two different quirks only the one
	 * picked at birth shows; a steady horse shows both steady copies.
	 */
	@Override
	public Map<Locus<?>, CopyRole> roles(Genome genome, Personality shown) {
		GenePair<Temperament> pair = genome.getOr(Loci.TEMPERAMENT, GenePair.of(Temperament.STEADY));
		return Map.of(Loci.TEMPERAMENT, shown == null ? CopyRole.NEITHER : CopyRole.where(pair, temperament(shown)));
	}

	/** The personality a temperament version stands for; they share their names. */
	public static Personality personality(Temperament temperament) {
		return Personality.valueOf(temperament.name());
	}

	/** The temperament version for a personality. */
	public static Temperament temperament(Personality personality) {
		return Temperament.valueOf(personality.name());
	}
}
