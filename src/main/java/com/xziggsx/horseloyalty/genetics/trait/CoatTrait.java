package com.xziggsx.horseloyalty.genetics.trait;

import com.xziggsx.horseloyalty.genetics.api.CopyRole;
import com.xziggsx.horseloyalty.genetics.core.Distribution;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Loci.Agouti;
import com.xziggsx.horseloyalty.genetics.core.Loci.Cream;
import com.xziggsx.horseloyalty.genetics.core.Loci.Gray;
import com.xziggsx.horseloyalty.genetics.core.Loci.RedBlack;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.NamedTrait;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.LookNames.CoatColour;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.animal.equine.Variant;

/**
 * A horse's coat colour (vanilla {@link Variant}), from four loci. First match wins:
 * <ol>
 *   <li>any GRAY: gray</li>
 *   <li>chestnut base (two RED) with two CREAM: white (cremello)</li>
 *   <li>chestnut base: creamy with one CREAM, else chestnut</li>
 *   <li>black pigment with two CREAM: creamy (perlino, smoky cream)</li>
 *   <li>black pigment with any BAY: creamy with one CREAM, else brown</li>
 *   <li>black pigment with SEAL_BROWN: creamy with one CREAM, else dark brown</li>
 *   <li>black pigment with two BLACK agouti: black, with or without one CREAM</li>
 * </ol>
 * The same rules give the coat's real name too (see {@link #colour}), which tells apart
 * colours that share a vanilla texture, such as palomino and buckskin.
 */
public final class CoatTrait implements Trait<Variant>, NamedTrait {
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
		return Distribution.single(colour(genome).texture());
	}

	@Override
	public String nameId(Genome genome) {
		return colour(genome).id();
	}

	/** The coat's real colour name, such as buckskin. Its {@link CoatColour#texture()} is what {@link #express} shows. */
	public CoatColour colour(Genome genome) {
		Base base = base(genome);
		int cream = cream(genome).count(Cream.CREAM);
		if (gray(genome).has(Gray.GRAY)) {
			return CoatColour.GREY;
		}
		return switch (base) {
			case CHESTNUT -> cream == 2 ? CoatColour.CREMELLO : cream == 1 ? CoatColour.PALOMINO : CoatColour.CHESTNUT;
			case BAY -> cream == 2 ? CoatColour.PERLINO : cream == 1 ? CoatColour.BUCKSKIN : CoatColour.BAY;
			case SEAL_BROWN -> cream == 2 ? CoatColour.SMOKY_CREAM : cream == 1 ? CoatColour.BROWN_BUCKSKIN : CoatColour.SEAL_BROWN;
			case BLACK -> cream == 2 ? CoatColour.SMOKY_CREAM : cream == 1 ? CoatColour.SMOKY_BLACK : CoatColour.BLACK;
		};
	}

	/**
	 * Gray hides everything else, so then only the gray copies show. Otherwise: cream copies,
	 * both red copies of a chestnut (or the black copies that make black pigment), and on black
	 * pigment the strongest agouti version present. A chestnut shows no agouti at all.
	 */
	@Override
	public Map<Locus<?>, CopyRole> roles(Genome genome, Variant shown) {
		Map<Locus<?>, CopyRole> roles = new LinkedHashMap<>();
		GenePair<Gray> gray = gray(genome);
		boolean isGray = gray.has(Gray.GRAY);
		GenePair<RedBlack> redBlack = redBlack(genome);
		Base base = base(genome);
		roles.put(Loci.RED_BLACK, isGray ? CopyRole.NEITHER
				: base == Base.CHESTNUT ? CopyRole.BOTH : CopyRole.where(redBlack, RedBlack.BLACK));
		roles.put(Loci.AGOUTI, isGray || base == Base.CHESTNUT ? CopyRole.NEITHER : agoutiRole(agouti(genome), base));
		roles.put(Loci.CREAM, isGray ? CopyRole.NEITHER : CopyRole.where(cream(genome), Cream.CREAM));
		roles.put(Loci.GRAY, CopyRole.where(gray, Gray.GRAY));
		return roles;
	}

	private static CopyRole agoutiRole(GenePair<Agouti> agouti, Base base) {
		return switch (base) {
			case BAY -> CopyRole.where(agouti, Agouti.BAY);
			case SEAL_BROWN -> CopyRole.where(agouti, Agouti.SEAL_BROWN);
			default -> CopyRole.BOTH;
		};
	}

	/** The base colour before cream and gray. */
	private enum Base {
		CHESTNUT,
		BAY,
		SEAL_BROWN,
		BLACK
	}

	private static Base base(Genome genome) {
		if (redBlack(genome).count(RedBlack.RED) == 2) {
			return Base.CHESTNUT;
		}
		GenePair<Agouti> agouti = agouti(genome);
		if (agouti.has(Agouti.BAY)) {
			return Base.BAY;
		}
		return agouti.has(Agouti.SEAL_BROWN) ? Base.SEAL_BROWN : Base.BLACK;
	}

	private static GenePair<RedBlack> redBlack(Genome genome) {
		return genome.getOr(Loci.RED_BLACK, GenePair.of(RedBlack.BLACK));
	}

	private static GenePair<Agouti> agouti(Genome genome) {
		return genome.getOr(Loci.AGOUTI, GenePair.of(Agouti.BAY));
	}

	private static GenePair<Cream> cream(Genome genome) {
		return genome.getOr(Loci.CREAM, GenePair.of(Cream.NONE));
	}

	private static GenePair<Gray> gray(Genome genome) {
		return genome.getOr(Loci.GRAY, GenePair.of(Gray.NONE));
	}
}
