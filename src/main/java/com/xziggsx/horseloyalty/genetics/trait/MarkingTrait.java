package com.xziggsx.horseloyalty.genetics.trait;

import com.xziggsx.horseloyalty.genetics.core.CopyRole;
import com.xziggsx.horseloyalty.genetics.core.Distribution;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Leopard;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.NamedTrait;
import com.xziggsx.horseloyalty.genetics.core.Pinto;
import com.xziggsx.horseloyalty.genetics.core.Socks;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.animal.equine.Markings;

/**
 * A horse's white markings (vanilla {@link Markings}), from three loci. First match wins:
 * any PINTO gives white patches (tobiano), two LEOPARD black dots (fewspot), one LEOPARD white
 * dots (leopard), two SOCKS white legs, and anything else no markings. The real pattern name
 * is {@link #pattern}.
 */
public final class MarkingTrait implements Trait<Markings>, NamedTrait {
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
		return Distribution.single(pattern(genome).texture());
	}

	@Override
	public String nameId(Genome genome) {
		return pattern(genome).id();
	}

	/** The pattern's real name, such as tobiano. Its {@link MarkingPattern#texture()} is what {@link #express} shows. */
	public MarkingPattern pattern(Genome genome) {
		if (pinto(genome).has(Pinto.PINTO)) {
			return MarkingPattern.TOBIANO;
		}
		int leopard = leopard(genome).count(Leopard.LEOPARD);
		if (leopard == 2) {
			return MarkingPattern.FEWSPOT;
		}
		if (leopard == 1) {
			return MarkingPattern.LEOPARD;
		}
		return socks(genome).count(Socks.SOCKS) == 2 ? MarkingPattern.WHITE_SOCKS : MarkingPattern.NONE;
	}

	/** Only the copies of the pattern that wins show: pinto, else leopard, else two socks. */
	@Override
	public Map<Locus<?>, CopyRole> roles(Genome genome, Markings shown) {
		MarkingPattern pattern = pattern(genome);
		Map<Locus<?>, CopyRole> roles = new LinkedHashMap<>();
		roles.put(Loci.PINTO, CopyRole.where(pinto(genome), Pinto.PINTO));
		roles.put(Loci.LEOPARD, pattern == MarkingPattern.TOBIANO ? CopyRole.NEITHER
				: CopyRole.where(leopard(genome), Leopard.LEOPARD));
		roles.put(Loci.SOCKS, pattern == MarkingPattern.WHITE_SOCKS ? CopyRole.BOTH : CopyRole.NEITHER);
		return roles;
	}

	private static GenePair<Pinto> pinto(Genome genome) {
		return genome.getOr(Loci.PINTO, GenePair.of(Pinto.NONE));
	}

	private static GenePair<Leopard> leopard(Genome genome) {
		return genome.getOr(Loci.LEOPARD, GenePair.of(Leopard.NONE));
	}

	private static GenePair<Socks> socks(Genome genome) {
		return genome.getOr(Loci.SOCKS, GenePair.of(Socks.NONE));
	}
}
