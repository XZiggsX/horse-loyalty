package com.xziggsx.horseloyalty.genetics.trait;

import com.xziggsx.horseloyalty.genetics.core.Agouti;
import com.xziggsx.horseloyalty.genetics.core.Cream;
import com.xziggsx.horseloyalty.genetics.core.Distribution;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Gray;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.RedBlack;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import java.util.List;
import net.minecraft.world.entity.animal.equine.Variant;

/**
 * A horse's coat colour (vanilla {@link Variant}), from four loci. First match wins:
 * <ol>
 *   <li>any GRAY: gray</li>
 *   <li>two CREAM: white</li>
 *   <li>chestnut base (two RED): creamy with one CREAM, else chestnut</li>
 *   <li>black pigment with any BAY: creamy with one CREAM, else brown</li>
 *   <li>black pigment with SEAL_BROWN: creamy with one CREAM, else dark brown</li>
 *   <li>black pigment with two BLACK agouti: black, with or without one CREAM</li>
 * </ol>
 */
public final class CoatTrait implements Trait<Variant> {
	private static final List<Locus<?>> LOCI = List.of(Loci.RED_BLACK, Loci.AGOUTI, Loci.CREAM, Loci.GRAY);

	@Override
	public String id() {
		return "coat";
	}

	@Override
	public List<Locus<?>> loci() {
		return LOCI;
	}

	@Override
	public Distribution<Variant> express(Genome genome) {
		return Distribution.single(coat(genome));
	}

	private static Variant coat(Genome genome) {
		GenePair<RedBlack> redBlack = genome.getOr(Loci.RED_BLACK, GenePair.of(RedBlack.BLACK));
		GenePair<Agouti> agouti = genome.getOr(Loci.AGOUTI, GenePair.of(Agouti.BAY));
		int cream = genome.getOr(Loci.CREAM, GenePair.of(Cream.NONE)).count(Cream.CREAM);
		if (genome.getOr(Loci.GRAY, GenePair.of(Gray.NONE)).has(Gray.GRAY)) {
			return Variant.GRAY;
		}
		if (cream == 2) {
			return Variant.WHITE;
		}
		if (redBlack.count(RedBlack.RED) == 2) {
			return cream == 1 ? Variant.CREAMY : Variant.CHESTNUT;
		}
		if (agouti.has(Agouti.BAY)) {
			return cream == 1 ? Variant.CREAMY : Variant.BROWN;
		}
		if (agouti.has(Agouti.SEAL_BROWN)) {
			return cream == 1 ? Variant.CREAMY : Variant.DARK_BROWN;
		}
		return Variant.BLACK;
	}
}
