package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Dice;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.GenomeFactory;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.personality.Personality;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;

/**
 * The two made-up parents of a wild herd. Every horse in a spawn group is a foal of the same
 * two founders, so a herd looks related: mostly the herd's vanilla coat, with the odd
 * surprise from hidden copies. The founders are kept on the group's
 * {@link Horse.HorseGroupData} (see {@code HorseGroupDataMixin}) while the group spawns.
 *
 * @param a the first founder
 * @param b the second founder
 */
public record HerdFounders(Genome a, Genome b) {
	/** Added to {@link Horse.HorseGroupData} by a mixin, so a group can hold its founders. */
	public interface Holder {
		HerdFounders horseloyalty$getFounders();

		void horseloyalty$setFounders(HerdFounders founders);
	}

	/**
	 * The founders of the group a horse spawns in, made the first time the group asks. Spawn
	 * data that isn't a horse group (or none) gets fresh founders with a random coat each time.
	 */
	public static HerdFounders of(SpawnGroupData groupData, RandomSource random, Dice dice) {
		if (!(groupData instanceof Holder holder)) {
			return create(Util.getRandom(Variant.values(), random), random, dice);
		}
		HerdFounders founders = holder.horseloyalty$getFounders();
		if (founders == null) {
			// Vanilla gives the whole group one coat; both founders get it.
			Variant variant = groupData instanceof Horse.HorseGroupData horseGroup
					? horseGroup.variant : Util.getRandom(Variant.values(), random);
			founders = create(variant, random, dice);
			holder.horseloyalty$setFounders(founders);
		}
		return founders;
	}

	/** Two founders with this coat, each with vanilla-style random markings and a rolled personality. */
	private static HerdFounders create(Variant variant, RandomSource random, Dice dice) {
		return new HerdFounders(founder(variant, random, dice), founder(variant, random, dice));
	}

	private static Genome founder(Variant variant, RandomSource random, Dice dice) {
		Map<Trait<?>, Object> look = new LinkedHashMap<>();
		look.put(HorseTraits.COAT, variant);
		look.put(HorseTraits.MARKING, Util.getRandom(Markings.values(), random));
		look.put(HorseTraits.TEMPERAMENT, Personality.roll(random));
		return GenomeFactory.forLook(look, dice);
	}
}
