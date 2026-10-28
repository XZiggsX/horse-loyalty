package com.xziggsx.horseloyalty.genetics.trait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xziggsx.horseloyalty.genetics.core.Agouti;
import com.xziggsx.horseloyalty.genetics.core.CopyRole;
import com.xziggsx.horseloyalty.genetics.core.Cream;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Gray;
import com.xziggsx.horseloyalty.genetics.core.Leopard;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.Pinto;
import com.xziggsx.horseloyalty.genetics.core.RedBlack;
import com.xziggsx.horseloyalty.genetics.core.Socks;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import org.junit.jupiter.api.Test;

/**
 * Coat and marking names and which copies show, checked against every combination of genes.
 * The coat and marking rules are written out again here, straight from the spec, so the
 * traits are checked against an independent copy.
 */
class CoatAndMarkingTest {
	private static final CoatTrait COAT = new CoatTrait();
	private static final MarkingTrait MARKING = new MarkingTrait();

	/** Every genome over these loci, with every ordered pair at each. */
	private static List<Genome> allGenomes(List<Locus<?>> loci) {
		List<Genome> genomes = new ArrayList<>(List.of(Genome.EMPTY));
		for (Locus<?> locus : loci) {
			genomes = expand(genomes, locus);
		}
		return genomes;
	}

	private static <A extends Enum<A>> List<Genome> expand(List<Genome> genomes, Locus<A> locus) {
		List<Genome> result = new ArrayList<>();
		for (Genome genome : genomes) {
			for (A first : locus.versions()) {
				for (A second : locus.versions()) {
					result.add(genome.with(locus, new GenePair<>(first, second)));
				}
			}
		}
		return result;
	}

	/** The coat rules from the spec, first match wins. */
	private static Variant specCoat(Genome genome) {
		GenePair<RedBlack> redBlack = genome.get(Loci.RED_BLACK);
		GenePair<Agouti> agouti = genome.get(Loci.AGOUTI);
		int cream = genome.get(Loci.CREAM).count(Cream.CREAM);
		if (genome.get(Loci.GRAY).has(Gray.GRAY)) {
			return Variant.GRAY;
		}
		if (cream == 2) {
			// Only a chestnut base goes white (cremello); perlino and smoky cream look creamy.
			return redBlack.count(RedBlack.RED) == 2 ? Variant.WHITE : Variant.CREAMY;
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

	/** The marking rules from the spec, first match wins. */
	private static Markings specMarkings(Genome genome) {
		if (genome.get(Loci.PINTO).has(Pinto.PINTO)) {
			return Markings.WHITE_FIELD;
		}
		int leopard = genome.get(Loci.LEOPARD).count(Leopard.LEOPARD);
		if (leopard == 2) {
			return Markings.BLACK_DOTS;
		}
		if (leopard == 1) {
			return Markings.WHITE_DOTS;
		}
		return genome.get(Loci.SOCKS).count(Socks.SOCKS) == 2 ? Markings.WHITE : Markings.NONE;
	}

	@Test
	void everyCoatCombinationMatchesTheSpecAndItsTexture() {
		Set<CoatColour> seen = EnumSet.noneOf(CoatColour.class);
		for (Genome genome : allGenomes(COAT.loci())) {
			CoatColour colour = COAT.colour(genome);
			seen.add(colour);
			assertEquals(specCoat(genome), colour.texture(), "texture of " + colour + " for " + genome);
			assertEquals(1.0, COAT.express(genome).chance(specCoat(genome)), 1e-9, genome.toString());
		}
		assertEquals(EnumSet.allOf(CoatColour.class), seen, "every coat colour can occur");
	}

	@Test
	void everyMarkingCombinationMatchesTheSpecAndItsTexture() {
		Set<MarkingPattern> seen = EnumSet.noneOf(MarkingPattern.class);
		for (Genome genome : allGenomes(MARKING.loci())) {
			MarkingPattern pattern = MARKING.pattern(genome);
			seen.add(pattern);
			assertEquals(specMarkings(genome), pattern.texture(), "texture of " + pattern + " for " + genome);
		}
		assertEquals(EnumSet.allOf(MarkingPattern.class), seen, "every marking pattern can occur");
	}

	private static Genome coat(RedBlack r1, RedBlack r2, Agouti a1, Agouti a2, Cream c1, Cream c2, Gray g1, Gray g2) {
		return Genome.EMPTY.with(Loci.RED_BLACK, new GenePair<>(r1, r2))
				.with(Loci.AGOUTI, new GenePair<>(a1, a2))
				.with(Loci.CREAM, new GenePair<>(c1, c2))
				.with(Loci.GRAY, new GenePair<>(g1, g2));
	}

	@Test
	void namedColours() {
		assertEquals(CoatColour.PALOMINO, COAT.colour(coat(RedBlack.RED, RedBlack.RED, Agouti.BAY, Agouti.BLACK,
				Cream.CREAM, Cream.NONE, Gray.NONE, Gray.NONE)));
		assertEquals(CoatColour.BUCKSKIN, COAT.colour(coat(RedBlack.BLACK, RedBlack.RED, Agouti.BAY, Agouti.BLACK,
				Cream.NONE, Cream.CREAM, Gray.NONE, Gray.NONE)));
		assertEquals(CoatColour.SMOKY_BLACK, COAT.colour(coat(RedBlack.BLACK, RedBlack.BLACK, Agouti.BLACK, Agouti.BLACK,
				Cream.CREAM, Cream.NONE, Gray.NONE, Gray.NONE)));
		assertEquals(CoatColour.CREMELLO, COAT.colour(coat(RedBlack.RED, RedBlack.RED, Agouti.BAY, Agouti.BAY,
				Cream.CREAM, Cream.CREAM, Gray.NONE, Gray.NONE)));
		assertEquals(CoatColour.PERLINO, COAT.colour(coat(RedBlack.BLACK, RedBlack.RED, Agouti.BAY, Agouti.SEAL_BROWN,
				Cream.CREAM, Cream.CREAM, Gray.NONE, Gray.NONE)));
		assertEquals(Variant.WHITE, CoatColour.CREMELLO.texture());
		assertEquals(Variant.CREAMY, CoatColour.PERLINO.texture(), "a perlino is cream-bodied, not white");
		assertEquals(Variant.CREAMY, CoatColour.SMOKY_CREAM.texture());
		assertEquals(CoatColour.GREY, COAT.colour(coat(RedBlack.RED, RedBlack.RED, Agouti.BAY, Agouti.BAY,
				Cream.CREAM, Cream.CREAM, Gray.NONE, Gray.GRAY)));
	}

	@Test
	void palominoRoles() {
		Genome genome = coat(RedBlack.RED, RedBlack.RED, Agouti.BAY, Agouti.BLACK, Cream.NONE, Cream.CREAM, Gray.NONE, Gray.NONE);
		Map<Locus<?>, CopyRole> roles = COAT.roles(genome, Variant.CREAMY);
		assertEquals(CopyRole.BOTH, roles.get(Loci.RED_BLACK));
		assertEquals(CopyRole.NEITHER, roles.get(Loci.AGOUTI), "a chestnut shows no agouti");
		assertEquals(new CopyRole(false, true), roles.get(Loci.CREAM));
		assertEquals(CopyRole.NEITHER, roles.get(Loci.GRAY));
	}

	@Test
	void bayCarryingRedAndBlackRoles() {
		Genome genome = coat(RedBlack.RED, RedBlack.BLACK, Agouti.BLACK, Agouti.BAY, Cream.NONE, Cream.NONE, Gray.NONE, Gray.NONE);
		Map<Locus<?>, CopyRole> roles = COAT.roles(genome, Variant.BROWN);
		assertEquals(new CopyRole(false, true), roles.get(Loci.RED_BLACK));
		assertEquals(new CopyRole(false, true), roles.get(Loci.AGOUTI));
		assertEquals(CopyRole.NEITHER, roles.get(Loci.CREAM));
	}

	@Test
	void blackShowsBothBlackCopies() {
		Genome genome = coat(RedBlack.BLACK, RedBlack.BLACK, Agouti.BLACK, Agouti.BLACK, Cream.NONE, Cream.NONE, Gray.NONE, Gray.NONE);
		Map<Locus<?>, CopyRole> roles = COAT.roles(genome, Variant.BLACK);
		assertEquals(CopyRole.BOTH, roles.get(Loci.RED_BLACK));
		assertEquals(CopyRole.BOTH, roles.get(Loci.AGOUTI));
	}

	@Test
	void grayHidesEverythingElse() {
		Genome genome = coat(RedBlack.BLACK, RedBlack.RED, Agouti.BAY, Agouti.BAY, Cream.CREAM, Cream.NONE, Gray.GRAY, Gray.NONE);
		Map<Locus<?>, CopyRole> roles = COAT.roles(genome, Variant.GRAY);
		assertEquals(new CopyRole(true, false), roles.get(Loci.GRAY));
		assertEquals(CopyRole.NEITHER, roles.get(Loci.RED_BLACK));
		assertEquals(CopyRole.NEITHER, roles.get(Loci.AGOUTI));
		assertEquals(CopyRole.NEITHER, roles.get(Loci.CREAM));
	}

	@Test
	void everyCoatGenomeShowsSomething() {
		for (Genome genome : allGenomes(COAT.loci())) {
			Map<Locus<?>, CopyRole> roles = COAT.roles(genome, COAT.colour(genome).texture());
			assertEquals(COAT.loci(), List.copyOf(roles.keySet()), "a role for every coat locus");
			assertTrue(roles.values().stream().anyMatch(role -> role.firstShows() || role.secondShows()),
					"something shows for " + genome);
		}
	}

	@Test
	void markingRoles() {
		Genome tobianoAndLeopard = Genome.EMPTY.with(Loci.PINTO, new GenePair<>(Pinto.NONE, Pinto.PINTO))
				.with(Loci.LEOPARD, new GenePair<>(Leopard.LEOPARD, Leopard.NONE))
				.with(Loci.SOCKS, GenePair.of(Socks.SOCKS));
		Map<Locus<?>, CopyRole> roles = MARKING.roles(tobianoAndLeopard, Markings.WHITE_FIELD);
		assertEquals(new CopyRole(false, true), roles.get(Loci.PINTO));
		assertEquals(CopyRole.NEITHER, roles.get(Loci.LEOPARD), "tobiano hides leopard");
		assertEquals(CopyRole.NEITHER, roles.get(Loci.SOCKS));

		Genome socks = Genome.EMPTY.with(Loci.PINTO, GenePair.of(Pinto.NONE))
				.with(Loci.LEOPARD, GenePair.of(Leopard.NONE))
				.with(Loci.SOCKS, GenePair.of(Socks.SOCKS));
		assertEquals(MarkingPattern.WHITE_SOCKS, MARKING.pattern(socks));
		assertEquals(CopyRole.BOTH, MARKING.roles(socks, Markings.WHITE).get(Loci.SOCKS));

		Genome carrier = socks.with(Loci.SOCKS, new GenePair<>(Socks.SOCKS, Socks.NONE));
		assertEquals(CopyRole.NEITHER, MARKING.roles(carrier, Markings.NONE).get(Loci.SOCKS), "one socks copy is only carried");
	}
}
