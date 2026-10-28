package com.xziggsx.horseloyalty.genetics.trait;

import com.xziggsx.horseloyalty.genetics.core.Distribution;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Leopard;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.Pinto;
import com.xziggsx.horseloyalty.genetics.core.Socks;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import java.util.List;
import net.minecraft.world.entity.animal.equine.Markings;

/**
 * A horse's white markings (vanilla {@link Markings}), from three loci. First match wins:
 * any PINTO gives white patches, two LEOPARD black dots, one LEOPARD white dots,
 * two SOCKS white legs, and anything else no markings.
 */
public final class MarkingTrait implements Trait<Markings> {
	private static final List<Locus<?>> LOCI = List.of(Loci.PINTO, Loci.LEOPARD, Loci.SOCKS);

	@Override
	public String id() {
		return "marking";
	}

	@Override
	public List<Locus<?>> loci() {
		return LOCI;
	}

	@Override
	public Distribution<Markings> express(Genome genome) {
		return Distribution.single(markings(genome));
	}

	private static Markings markings(Genome genome) {
		if (genome.getOr(Loci.PINTO, GenePair.of(Pinto.NONE)).has(Pinto.PINTO)) {
			return Markings.WHITE_FIELD;
		}
		int leopard = genome.getOr(Loci.LEOPARD, GenePair.of(Leopard.NONE)).count(Leopard.LEOPARD);
		if (leopard == 2) {
			return Markings.BLACK_DOTS;
		}
		if (leopard == 1) {
			return Markings.WHITE_DOTS;
		}
		if (genome.getOr(Loci.SOCKS, GenePair.of(Socks.NONE)).count(Socks.SOCKS) == 2) {
			return Markings.WHITE;
		}
		return Markings.NONE;
	}
}
