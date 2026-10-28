package com.xziggsx.horseloyalty.horseinfo;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.genetics.core.CopyRole;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.NamedTrait;
import com.xziggsx.horseloyalty.genetics.view.GeneticsView;
import com.xziggsx.horseloyalty.genetics.view.ShownTrait;
import com.xziggsx.horseloyalty.genetics.world.Lineage;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.GeneRow;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.Identity;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.LoyaltyLine;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.Parents;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.StatLine;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.TraitLine;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.personality.Personality;
import com.xziggsx.horseloyalty.util.HorseUnits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Builds the {@link HorseProfile} the horse info screen shows. The one place that reads the
 * horse, its genes, loyalty and personality for the screen: the screen only sees the profile,
 * so either side can change without the other. Works on the client, where genes, lineage,
 * personality and loyalty levels are synced.
 */
public final class HorseProfiles {
	private HorseProfiles() {
	}

	/** Whether this horse has an info screen: genetics is on (the server's setting) and it has genes. */
	public static boolean available(AbstractHorse horse) {
		return HorseLoyaltyConfig.get().geneticsEnabled() && Loyalty.appliesTo(horse) && GeneticsView.genome(horse).isPresent();
	}

	/** The profile of this horse as the player may see it. Call only when {@link #available}. */
	public static HorseProfile of(AbstractHorse horse, Player player, RevealPolicy policy) {
		Genome genome = GeneticsView.genome(horse).orElse(Genome.EMPTY);
		List<ShownTrait<?>> traits = GeneticsView.shownTraits(horse);
		return new HorseProfile(
				reveal(policy, InfoTopic.IDENTITY, horse, player, () -> identity(horse)),
				reveal(policy, InfoTopic.LOYALTY, horse, player, () -> loyalty(horse, player)),
				reveal(policy, InfoTopic.STATS, horse, player, () -> stats(horse)),
				reveal(policy, InfoTopic.PARENTS, horse, player, () -> parents(horse)),
				reveal(policy, InfoTopic.LOOKS, horse, player, () -> looks(genome, traits)),
				reveal(policy, InfoTopic.GENES, horse, player, () -> genes(genome, traits)));
	}

	/**
	 * Everything the profile is built from, as one comparable value: the screen rebuilds the
	 * profile only when this changes. Cheap, no gene reading.
	 */
	public static Object changeKey(AbstractHorse horse, Player player) {
		return List.of(horse.getHealth(), horse.getMaxHealth(),
				horse.getAttributeValue(Attributes.MOVEMENT_SPEED), horse.getAttributeValue(Attributes.JUMP_STRENGTH),
				Loyalty.level(horse, player), Personality.of(horse),
				GeneticsView.genome(horse).map(Object.class::cast).orElse(""),
				GeneticsView.lineage(horse).map(Object.class::cast).orElse(""));
	}

	private static <T> Shown<T> reveal(RevealPolicy policy, InfoTopic topic, AbstractHorse horse, Player player,
			Supplier<T> value) {
		Optional<Component> reason = policy.lockReason(topic, horse, player);
		return reason.isPresent() ? Shown.locked(reason.get()) : Shown.visible(value.get());
	}

	private static Identity identity(AbstractHorse horse) {
		return new Identity(horse.getType().getDescription(), Personality.of(horse));
	}

	private static LoyaltyLine loyalty(AbstractHorse horse, Player player) {
		int level = Loyalty.level(horse, player);
		return new LoyaltyLine(level, Loyalty.MAX_LEVEL, Loyalty.levelName(level));
	}

	/** Health, speed and jump. Add a line here for a new stat; the screen formats it by id. */
	private static List<StatLine> stats(AbstractHorse horse) {
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
		return List.of(
				new StatLine("health", horse.getHealth(), horse.getMaxHealth(),
						HorseUnits.fill(horse.getHealth(), 0, horse.getMaxHealth())),
				new StatLine("speed", HorseUnits.blocksPerSecond(speed), HorseUnits.blocksPerSecond(HorseUnits.MAX_SPEED),
						HorseUnits.fill(speed, HorseUnits.MIN_SPEED, HorseUnits.MAX_SPEED)),
				new StatLine("jump", HorseUnits.jumpBlocks(jump), HorseUnits.jumpBlocks(HorseUnits.MAX_JUMP),
						HorseUnits.fill(jump, HorseUnits.MIN_JUMP, HorseUnits.MAX_JUMP)));
	}

	private static Parents parents(AbstractHorse horse) {
		Optional<Lineage> lineage = GeneticsView.lineage(horse).filter(Lineage::hasParents);
		return lineage.map(value -> new Parents(true, value.nameA(), value.nameB()))
				.orElse(new Parents(false, Optional.empty(), Optional.empty()));
	}

	/** The real name of each shown trait that has one (coat, markings). */
	private static List<TraitLine> looks(Genome genome, List<ShownTrait<?>> traits) {
		List<TraitLine> lines = new ArrayList<>();
		for (ShownTrait<?> shown : traits) {
			if (shown.trait() instanceof NamedTrait named) {
				lines.add(new TraitLine(shown.trait().id(), named.nameId(genome)));
			}
		}
		return lines;
	}

	/** A row per locus of each shown trait, with which copies decide what the horse shows. */
	private static List<GeneRow> genes(Genome genome, List<ShownTrait<?>> traits) {
		List<GeneRow> rows = new ArrayList<>();
		for (ShownTrait<?> shown : traits) {
			addRows(rows, genome, shown);
		}
		return rows;
	}

	private static <T> void addRows(List<GeneRow> rows, Genome genome, ShownTrait<T> shown) {
		Map<Locus<?>, CopyRole> roles = shown.trait().roles(genome, shown.current());
		for (Locus<?> locus : shown.trait().loci()) {
			GenePair<?> pair = genome.pairs().get(locus);
			if (pair != null) {
				rows.add(new GeneRow(locus.id(), Locus.name(pair.first()), Locus.name(pair.second()),
						roles.getOrDefault(locus, CopyRole.NEITHER)));
			}
		}
	}
}
