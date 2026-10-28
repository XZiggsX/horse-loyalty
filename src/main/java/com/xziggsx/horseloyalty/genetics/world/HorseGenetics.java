package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.genetics.GeneticsSettings;
import com.xziggsx.horseloyalty.genetics.core.Dice;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.GenomeFactory;
import com.xziggsx.horseloyalty.genetics.core.Inheritance;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Hidden genes behind a horse's coat, markings and personality. The only way in, through the horse events:
 * breeding ({@link #onBred}), wild spawns ({@link #onSpawn}) and the once-a-second check
 * ({@link #ensureAssigned}). It only runs while genetics is on (the horse events are gated by its switch), and only on the server.
 * <p>
 * Genes don't touch stats: health, speed and jump still average the parents as in vanilla.
 */
public final class HorseGenetics {
	private HorseGenetics() {
	}

	/** Game randomness as genetics {@link Dice}. */
	public static Dice dice(RandomSource random) {
		return Dice.of(random::nextInt, random::nextDouble);
	}

	/**
	 * Sets up a bred foal: crosses the parents' genes, records them and their names as its
	 * lineage, and makes the foal show what its genes say. Parents without genes get some
	 * that fit their look first. Called on the server before the foal is added to the world.
	 */
	public static void onBred(AbstractHorse foal, AbstractHorse parentA, AbstractHorse parentB) {
		if (!HorseScope.inScope(foal) || !HorseScope.inScope(parentA) || !HorseScope.inScope(parentB)) {
			return;
		}
		Dice dice = dice(foal.getRandom());
		Genome a = genomeOf(parentA);
		Genome b = genomeOf(parentB);
		Genome genome = GenomeFactory.fillMissing(
				Inheritance.cross(a, b, GeneticsSettings.get().mutationChance(), dice), dice);
		foal.setAttached(GeneticsData.GENOME, genome);
		foal.setAttached(GeneticsData.LINEAGE, new Lineage(Optional.of(a), Optional.of(b), customName(parentA), customName(parentB)));
		express(foal, genome, dice);
	}

	/**
	 * Gives a newly spawned horse genes as a foal of its herd's founders (see {@link HerdFounders}),
	 * and makes it show them. Its lineage is the founders.
	 *
	 * @param groupData the spawn group data shared by the herd, or null
	 */
	public static void onSpawn(AbstractHorse horse, SpawnGroupData groupData) {
		if (!HorseScope.inScope(horse) || horse.level().isClientSide()) {
			return;
		}
		RandomSource random = horse.getRandom();
		Dice dice = dice(random);
		HerdFounders founders = HerdFounders.of(groupData, random, dice);
		Genome genome = GenomeFactory.fillMissing(
				Inheritance.cross(founders.a(), founders.b(), GeneticsSettings.get().mutationChance(), dice), dice);
		horse.setAttached(GeneticsData.GENOME, genome);
		horse.setAttached(GeneticsData.LINEAGE,
				new Lineage(Optional.of(founders.a()), Optional.of(founders.b()), Optional.empty(), Optional.empty()));
		express(horse, genome, dice);
	}

	/**
	 * Keeps a horse's genes in step with its look. Called once a second for every horse, after
	 * personality's own check:
	 * <ul>
	 *   <li>no genes yet (an old horse, or genetics was just turned on): builds genes that fit
	 *   what it shows now, so nothing about it changes;</li>
	 *   <li>missing loci (added in a later version): fills them in;</li>
	 *   <li>a trait its genes can't show (changed by a command or another mod): re-rolls just
	 *   that trait's genes to fit.</li>
	 * </ul>
	 * Only a change is saved and synced.
	 */
	public static void ensureAssigned(AbstractHorse horse) {
		if (!HorseScope.inScope(horse)) {
			return;
		}
		Genome genome = horse.getAttached(GeneticsData.GENOME);
		if (genome == null) {
			horse.setAttached(GeneticsData.GENOME, buildForLook(horse));
			return;
		}
		Dice dice = dice(horse.getRandom());
		Genome fitted = GenomeFactory.fillMissing(genome, dice);
		for (TraitBinding<?> binding : Bindings.applicable(horse)) {
			fitted = refitToCurrent(fitted, binding, horse, dice);
		}
		if (!fitted.equals(genome)) {
			horse.setAttached(GeneticsData.GENOME, fitted);
		}
	}

	/** The horse's genes, made to fit its look (and saved) if it has none yet. Server side. */
	public static Genome genomeOf(AbstractHorse horse) {
		Genome genome = horse.getAttached(GeneticsData.GENOME);
		if (genome == null) {
			genome = buildForLook(horse);
			horse.setAttached(GeneticsData.GENOME, genome);
		}
		return genome;
	}

	/** Replaces a horse's genes and makes it show them, as for the dev command. Server side. */
	public static void setGenome(AbstractHorse horse, Genome genome) {
		Dice dice = dice(horse.getRandom());
		Genome complete = GenomeFactory.fillMissing(genome, dice);
		horse.setAttached(GeneticsData.GENOME, complete);
		express(horse, complete, dice);
	}

	/** Makes the horse show what its genes say, sampling once where genes compete. */
	private static void express(AbstractHorse horse, Genome genome, Dice dice) {
		for (TraitBinding<?> binding : Bindings.applicable(horse)) {
			expressOne(binding, horse, genome, dice);
		}
	}

	private static <T> void expressOne(TraitBinding<T> binding, AbstractHorse horse, Genome genome, Dice dice) {
		T value = binding.trait().express(genome).sample(dice);
		if (value != null) {
			binding.apply(horse, value);
		}
	}

	/** Random genes that fit everything the horse shows now. */
	private static Genome buildForLook(AbstractHorse horse) {
		Map<Trait<?>, Object> look = new LinkedHashMap<>();
		for (TraitBinding<?> binding : Bindings.applicable(horse)) {
			Object current = binding.current(horse);
			if (current != null) {
				look.put(binding.trait(), current);
			}
		}
		return GenomeFactory.forLook(look, dice(horse.getRandom()));
	}

	private static <T> Genome refitToCurrent(Genome genome, TraitBinding<T> binding, AbstractHorse horse, Dice dice) {
		T current = binding.current(horse);
		return current == null ? genome : GenomeFactory.refit(genome, binding.trait(), current, dice);
	}

	private static Optional<String> customName(AbstractHorse horse) {
		Component name = horse.getCustomName();
		return name == null ? Optional.empty() : Optional.of(name.getString());
	}
}
