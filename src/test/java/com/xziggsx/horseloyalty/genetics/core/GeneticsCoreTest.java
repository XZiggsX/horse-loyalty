package com.xziggsx.horseloyalty.genetics.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xziggsx.horseloyalty.genetics.trait.CoatTrait;
import com.xziggsx.horseloyalty.genetics.trait.MarkingTrait;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import org.junit.jupiter.api.Test;

/**
 * Tests for the genetics core, using the coat and marking traits (they only need the vanilla
 * enums) and a stand-in for the temperament trait, which needs the running game.
 */
class GeneticsCoreTest {
	private static final double EPSILON = 1e-9;
	private static final CoatTrait COAT = new CoatTrait();
	private static final MarkingTrait MARKING = new MarkingTrait();
	private static final QuirkTrait QUIRK = new QuirkTrait();

	/** Plain genes everywhere: bay base, no cream, gray, pinto, leopard or socks, steady. */
	private static Genome plain() {
		return Genome.EMPTY
				.with(Loci.RED_BLACK, GenePair.of(RedBlack.BLACK))
				.with(Loci.AGOUTI, GenePair.of(Agouti.BAY))
				.with(Loci.CREAM, GenePair.of(Cream.NONE))
				.with(Loci.GRAY, GenePair.of(Gray.NONE))
				.with(Loci.PINTO, GenePair.of(Pinto.NONE))
				.with(Loci.LEOPARD, GenePair.of(Leopard.NONE))
				.with(Loci.SOCKS, GenePair.of(Socks.NONE))
				.with(Loci.TEMPERAMENT, GenePair.of(Temperament.STEADY));
	}

	private static Genome palomino() {
		return plain()
				.with(Loci.RED_BLACK, GenePair.of(RedBlack.RED))
				.with(Loci.CREAM, new GenePair<>(Cream.CREAM, Cream.NONE));
	}

	@Test
	void palominoTimesPalomino() {
		Genome palomino = palomino();
		assertEquals(Distribution.single(Variant.CREAMY).outcomes(), COAT.express(palomino).outcomes());
		Distribution<Variant> odds = CrossPredictor.predict(palomino, palomino, COAT, 0);
		assertEquals(0.25, odds.chance(Variant.CHESTNUT), EPSILON);
		assertEquals(0.50, odds.chance(Variant.CREAMY), EPSILON);
		assertEquals(0.25, odds.chance(Variant.WHITE), EPSILON);
		assertEquals(3, odds.outcomes().size());
	}

	@Test
	void baysCarryingRed() {
		Genome bay = plain().with(Loci.RED_BLACK, new GenePair<>(RedBlack.BLACK, RedBlack.RED));
		assertEquals(1.0, COAT.express(bay).chance(Variant.BROWN), EPSILON);
		Distribution<Variant> odds = CrossPredictor.predict(bay, bay, COAT, 0);
		assertEquals(0.75, odds.chance(Variant.BROWN), EPSILON);
		assertEquals(0.25, odds.chance(Variant.CHESTNUT), EPSILON);
	}

	@Test
	void whiteDotsTimesWhiteDots() {
		Genome dots = plain().with(Loci.LEOPARD, new GenePair<>(Leopard.LEOPARD, Leopard.NONE));
		assertEquals(1.0, MARKING.express(dots).chance(Markings.WHITE_DOTS), EPSILON);
		Distribution<Markings> odds = CrossPredictor.predict(dots, dots, MARKING, 0);
		assertEquals(0.25, odds.chance(Markings.BLACK_DOTS), EPSILON);
		assertEquals(0.50, odds.chance(Markings.WHITE_DOTS), EPSILON);
		assertEquals(0.25, odds.chance(Markings.NONE), EPSILON);
	}

	@Test
	void braveCarrierTimesSteady() {
		Genome carrier = plain().with(Loci.TEMPERAMENT, new GenePair<>(Temperament.BRAVE, Temperament.STEADY));
		Distribution<Temperament> odds = CrossPredictor.predict(carrier, plain(), QUIRK, 0);
		assertEquals(0.5, odds.chance(Temperament.BRAVE), EPSILON);
		assertEquals(0.5, odds.chance(Temperament.STEADY), EPSILON);
	}

	@Test
	void braveTimesBraveBreedsTrue() {
		Genome brave = plain().with(Loci.TEMPERAMENT, GenePair.of(Temperament.BRAVE));
		Distribution<Temperament> odds = CrossPredictor.predict(brave, brave, QUIRK, 0);
		assertEquals(1.0, odds.chance(Temperament.BRAVE), EPSILON);
	}

	@Test
	void twoDifferentQuirksSplitEvenly() {
		Genome mixed = plain().with(Loci.TEMPERAMENT, new GenePair<>(Temperament.BRAVE, Temperament.LAZY));
		Distribution<Temperament> shown = QUIRK.express(mixed);
		assertEquals(0.5, shown.chance(Temperament.BRAVE), EPSILON);
		assertEquals(0.5, shown.chance(Temperament.LAZY), EPSILON);
	}

	@Test
	void mutationKeepsOddsSummingToOne() {
		Distribution<Variant> odds = CrossPredictor.predict(palomino(), palomino(), COAT, 0.05);
		double total = odds.outcomes().values().stream().mapToDouble(Double::doubleValue).sum();
		assertEquals(1.0, total, 1e-9);
		// Gray can only come from a mutation: each of the 4 passed gray copies has 5% * 1/2 to turn gray.
		assertEquals(1 - Math.pow(1 - 0.025, 2), odds.chance(Variant.GRAY), 1e-9);
	}

	@Test
	void simulationMatchesPrediction() {
		Genome a = palomino().with(Loci.LEOPARD, new GenePair<>(Leopard.LEOPARD, Leopard.NONE))
				.with(Loci.TEMPERAMENT, new GenePair<>(Temperament.BRAVE, Temperament.CURIOUS));
		Genome b = plain().with(Loci.RED_BLACK, new GenePair<>(RedBlack.BLACK, RedBlack.RED))
				.with(Loci.AGOUTI, new GenePair<>(Agouti.BAY, Agouti.SEAL_BROWN))
				.with(Loci.CREAM, new GenePair<>(Cream.CREAM, Cream.NONE))
				.with(Loci.SOCKS, GenePair.of(Socks.SOCKS))
				.with(Loci.TEMPERAMENT, new GenePair<>(Temperament.LAZY, Temperament.STEADY));
		double mutation = 0.01;
		int count = 10_000;
		Dice dice = Dice.of(new Random(12345));
		for (Trait<?> trait : List.of(COAT, MARKING, QUIRK)) {
			assertSimulationClose(trait, a, b, mutation, count, dice);
		}
	}

	private static <T> void assertSimulationClose(Trait<T> trait, Genome a, Genome b, double mutation, int count, Dice dice) {
		Map<T, Integer> counts = new HashMap<>();
		for (int i = 0; i < count; i++) {
			counts.merge(trait.express(Inheritance.cross(a, b, mutation, dice)).sample(dice), 1, Integer::sum);
		}
		Distribution<T> predicted = CrossPredictor.predict(a, b, trait, mutation);
		for (T value : predicted.outcomes().keySet()) {
			double simulated = counts.getOrDefault(value, 0) * 100.0 / count;
			double expected = predicted.chance(value) * 100;
			assertTrue(Math.abs(simulated - expected) <= 3,
					trait.id() + " " + value + ": simulated " + simulated + "%, predicted " + expected + "%");
		}
		for (T value : counts.keySet()) {
			assertTrue(predicted.chance(value) > 0, trait.id() + " " + value + " came up but was predicted impossible");
		}
	}

	@Test
	void forLookGivesEveryCoatAndMarking() {
		Dice dice = Dice.of(new Random(42));
		for (Variant variant : Variant.values()) {
			for (Markings markings : Markings.values()) {
				for (int i = 0; i < 20; i++) {
					Map<Trait<?>, Object> look = new LinkedHashMap<>();
					look.put(COAT, variant);
					look.put(MARKING, markings);
					look.put(QUIRK, Temperament.CURIOUS);
					Genome genome = GenomeFactory.forLook(look, dice);
					assertTrue(genome.isComplete(), "incomplete genome " + genome);
					assertEquals(1.0, COAT.express(genome).chance(variant), EPSILON, variant + " from " + genome);
					assertEquals(1.0, MARKING.express(genome).chance(markings), EPSILON, markings + " from " + genome);
					assertTrue(QUIRK.canExpress(genome, Temperament.CURIOUS), "curious from " + genome);
				}
			}
		}
	}

	@Test
	void refitOnlyChangesThatTraitsLoci() {
		Dice dice = Dice.of(new Random(7));
		Genome genome = palomino().with(Loci.PINTO, new GenePair<>(Pinto.PINTO, Pinto.NONE));
		Genome refitted = GenomeFactory.refit(genome, COAT, Variant.DARK_BROWN, dice);
		assertEquals(1.0, COAT.express(refitted).chance(Variant.DARK_BROWN), EPSILON);
		assertEquals(genome.get(Loci.PINTO), refitted.get(Loci.PINTO));
		assertEquals(genome.get(Loci.TEMPERAMENT), refitted.get(Loci.TEMPERAMENT));
		// Already fitting: unchanged.
		assertEquals(genome, GenomeFactory.refit(genome, COAT, Variant.CREAMY, dice));
	}

	@Test
	void savedFormRoundTripsAndSkipsJunk() {
		Genome genome = palomino();
		assertEquals(genome, Genome.fromStrings(genome.toStrings()));
		Map<String, String> junk = new LinkedHashMap<>(genome.toStrings());
		junk.put("wings", "big/small");
		junk.put("gray", "purple/none");
		junk.put("cream", "cream");
		Genome read = Genome.fromStrings(junk);
		assertEquals(genome.get(Loci.RED_BLACK), read.get(Loci.RED_BLACK));
		assertTrue(!read.has(Loci.GRAY) && !read.has(Loci.CREAM));
		assertTrue(GenomeFactory.fillMissing(read, Dice.of(new Random(1))).isComplete());
	}

	@Test
	void defaultRolesGuessFromWhatChanges() {
		// The stand-in trait has no roles of its own, so this is the generic guess.
		Genome brave = plain().with(Loci.TEMPERAMENT, new GenePair<>(Temperament.BRAVE, Temperament.STEADY));
		// Coarse: swapping the steady copy for another quirk would change things too, so it counts.
		// This is why the real traits give their own roles.
		assertEquals(CopyRole.BOTH, QUIRK.roles(brave, Temperament.BRAVE).get(Loci.TEMPERAMENT));
		assertTrue(!QUIRK.roles(plain(), Temperament.STEADY).containsKey(Loci.CREAM), "only this trait's loci");
		Genome mixed = plain().with(Loci.TEMPERAMENT, new GenePair<>(Temperament.BRAVE, Temperament.LAZY));
		assertEquals(CopyRole.BOTH, QUIRK.roles(mixed, Temperament.LAZY).get(Loci.TEMPERAMENT));
	}

	/**
	 * The temperament rules on the core enum. The real trait shows a Personality, whose class
	 * registers game data when it loads, so it can't be used outside the game.
	 */
	private static final class QuirkTrait implements Trait<Temperament> {
		@Override
		public String id() {
			return "quirk";
		}

		@Override
		public List<Locus<?>> loci() {
			return List.of(Loci.TEMPERAMENT);
		}

		@Override
		public Distribution<Temperament> express(Genome genome) {
			GenePair<Temperament> pair = genome.getOr(Loci.TEMPERAMENT, GenePair.of(Temperament.STEADY));
			if (pair.first() == pair.second() || pair.second() == Temperament.STEADY) {
				return Distribution.single(pair.first());
			}
			if (pair.first() == Temperament.STEADY) {
				return Distribution.single(pair.second());
			}
			Map<Temperament, Double> split = new LinkedHashMap<>();
			split.put(pair.first(), 0.5);
			split.put(pair.second(), 0.5);
			return Distribution.of(split);
		}
	}
}
