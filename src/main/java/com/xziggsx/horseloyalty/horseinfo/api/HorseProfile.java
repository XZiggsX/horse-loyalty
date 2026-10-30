package com.xziggsx.horseloyalty.horseinfo.api;

import com.xziggsx.horseloyalty.genetics.api.CopyRole;
import com.xziggsx.horseloyalty.personality.api.Personality;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Everything the horse info screen shows about one horse, as plain values. Built by horse info
 * from what each feature's {@link InfoProvider} adds; screens only ever read this, never the horse or its genes. Names of
 * genes, coats and patterns are ids, so the screen picks how to name them.
 *
 * @param identity kind and personality
 * @param loyalty  the viewing player's loyalty level
 * @param stats    stat bars in display order
 * @param parents  the parents' names, if recorded
 * @param looks    real names of what the horse shows, such as its coat; empty for donkeys and mules
 * @param genes    one row per gene the horse shows, in trait order
 * @param tastes   the foods the horse is known to love and dislike
 */
public record HorseProfile(
		Shown<Identity> identity,
		Shown<LoyaltyLine> loyalty,
		Shown<List<StatLine>> stats,
		Shown<Parents> parents,
		Shown<List<TraitLine>> looks,
		Shown<List<GeneRow>> genes,
		Shown<TastesLine> tastes) {

	/**
	 * @param kind        the kind of horse, such as "Donkey"
	 * @param personality what the horse shows, if the personality feature is on
	 */
	public record Identity(Component kind, Optional<Personality> personality) {
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

	/**
	 * @param loves          the revealed favorite foods' names
	 * @param hiddenLoves    favorites nobody has found out yet
	 * @param dislikes       the revealed disliked foods' names
	 * @param hiddenDislikes dislikes nobody has found out yet
	 */
	public record TastesLine(List<Component> loves, int hiddenLoves, List<Component> dislikes, int hiddenDislikes) {
	}

	/**
	 * Collects what each feature adds to a profile. A part nobody adds is off. Built once per
	 * profile by horse info, which then applies the reveal policy (see {@link #build}).
	 */
	public static final class Builder {
		private final Component kind;
		private final Supplier<List<StatLine>> stats;
		private Supplier<Personality> personality;
		private Supplier<LoyaltyLine> loyalty;
		private Supplier<Parents> parents;
		private Supplier<List<TraitLine>> looks;
		private Supplier<List<GeneRow>> genes;
		private Supplier<TastesLine> tastes;

		/** Starts a profile with what every horse has: its kind and its stats. */
		public Builder(Component kind, Supplier<List<StatLine>> stats) {
			this.kind = kind;
			this.stats = stats;
		}

		/** The personality, shown next to the kind. */
		public Builder personality(Supplier<Personality> value) {
			personality = value;
			return this;
		}

		/** The viewing player's loyalty line. */
		public Builder loyalty(Supplier<LoyaltyLine> value) {
			loyalty = value;
			return this;
		}

		/** The parents' names. */
		public Builder parents(Supplier<Parents> value) {
			parents = value;
			return this;
		}

		/** The real names of what the horse shows. */
		public Builder looks(Supplier<List<TraitLine>> value) {
			looks = value;
			return this;
		}

		/** The gene table. */
		public Builder genes(Supplier<List<GeneRow>> value) {
			genes = value;
			return this;
		}

		/** The foods the horse is known to love and dislike. */
		public Builder tastes(Supplier<TastesLine> value) {
			tastes = value;
			return this;
		}

		/** The profile as this player may see it: parts nobody added are off, the rest go through the policy. */
		public HorseProfile build(RevealPolicy policy, AbstractHorse horse, Player player) {
			Supplier<Personality> shownPersonality = personality;
			return new HorseProfile(
					reveal(policy, InfoTopic.IDENTITY, horse, player, () -> new Identity(kind,
							shownPersonality == null ? Optional.empty() : Optional.of(shownPersonality.get()))),
					reveal(policy, InfoTopic.LOYALTY, horse, player, loyalty),
					reveal(policy, InfoTopic.STATS, horse, player, stats),
					reveal(policy, InfoTopic.PARENTS, horse, player, parents),
					reveal(policy, InfoTopic.LOOKS, horse, player, looks),
					reveal(policy, InfoTopic.GENES, horse, player, genes),
					reveal(policy, InfoTopic.TASTES, horse, player, tastes));
		}

		private static <T> Shown<T> reveal(RevealPolicy policy, InfoTopic topic, AbstractHorse horse, Player player,
				Supplier<T> value) {
			if (value == null) {
				return Shown.off();
			}
			Optional<Component> reason = policy.lockReason(topic, horse, player);
			return reason.isPresent() ? Shown.locked(reason.get()) : Shown.visible(value.get());
		}
	}
}
