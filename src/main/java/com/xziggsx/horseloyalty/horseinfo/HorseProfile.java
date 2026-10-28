package com.xziggsx.horseloyalty.horseinfo;

import com.xziggsx.horseloyalty.genetics.core.CopyRole;
import com.xziggsx.horseloyalty.personality.Personality;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * Everything the horse info screen shows about one horse, as plain values. Built by
 * {@link HorseProfiles}; screens only ever read this, never the horse or its genes. Names of
 * genes, coats and patterns are ids, so the screen picks how to name them.
 *
 * @param identity name, kind and personality
 * @param loyalty  the viewing player's loyalty level
 * @param stats    stat bars in display order
 * @param parents  the parents' names, if recorded
 * @param looks    real names of what the horse shows, such as its coat; empty for donkeys and mules
 * @param genes    one row per gene the horse shows, in trait order
 */
public record HorseProfile(
		Shown<Identity> identity,
		Shown<LoyaltyLine> loyalty,
		Shown<List<StatLine>> stats,
		Shown<Parents> parents,
		Shown<List<TraitLine>> looks,
		Shown<List<GeneRow>> genes) {

	/**
	 * @param name        the horse's name (its custom name, or its kind)
	 * @param kind        the kind of horse, such as "Donkey"
	 * @param personality what the horse shows
	 */
	public record Identity(Component name, Component kind, Personality personality) {
	}

	/**
	 * @param level the viewing player's level, 0 for untamed
	 * @param max   the highest level
	 * @param name  the level's name, such as "Trusting"
	 */
	public record LoyaltyLine(int level, int max, Component name) {
	}

	/**
	 * One stat bar. The screen formats it by id.
	 *
	 * @param id    "health", "speed" or "jump"
	 * @param value the number to show (health points, blocks per second, blocks)
	 * @param max   the most it can be for this horse (max health), or the top of the range
	 * @param fill  how full the bar is, from 0 to 1
	 */
	public record StatLine(String id, double value, double max, double fill) {
	}

	/**
	 * @param recorded whether the horse has a lineage at all (horses from before genetics don't)
	 * @param nameA    the first parent's name, if it had one
	 * @param nameB    the second parent's name, if it had one
	 */
	public record Parents(boolean recorded, Optional<String> nameA, Optional<String> nameB) {
	}

	/**
	 * @param traitId the trait, such as "coat"
	 * @param valueId the real name's id, such as "buckskin"
	 */
	public record TraitLine(String traitId, String valueId) {
	}

	/**
	 * @param locusId  the gene, such as "agouti"
	 * @param firstId  the first copy's version, such as "seal_brown"
	 * @param secondId the second copy's version
	 * @param role     which copies decide the look
	 */
	public record GeneRow(String locusId, String firstId, String secondId, CopyRole role) {
	}
}
