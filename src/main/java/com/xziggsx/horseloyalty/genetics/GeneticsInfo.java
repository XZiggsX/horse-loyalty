package com.xziggsx.horseloyalty.genetics;

import com.xziggsx.horseloyalty.genetics.api.CopyRole;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.NamedTrait;
import com.xziggsx.horseloyalty.genetics.view.GeneticsView;
import com.xziggsx.horseloyalty.genetics.view.ShownTrait;
import com.xziggsx.horseloyalty.genetics.world.Lineage;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.GeneRow;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.Parents;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.TraitLine;
import com.xziggsx.horseloyalty.horseinfo.api.InfoProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** Genetics' parts of the horse info screen: the parents, the real names of the looks, and the genes. */
final class GeneticsInfo implements InfoProvider {
	@Override
	public void contribute(AbstractHorse horse, Player player, HorseProfile.Builder profile) {
		Genome genome = GeneticsView.genome(horse).orElse(Genome.EMPTY);
		List<ShownTrait<?>> traits = GeneticsView.shownTraits(horse);
		profile.parents(() -> parents(horse))
				.looks(() -> looks(genome, traits))
				.genes(() -> genes(genome, traits));
	}

	@Override
	public List<Object> changeKey(AbstractHorse horse, Player player) {
		return List.of(GeneticsView.genome(horse).map(Object.class::cast).orElse(""),
				GeneticsView.lineage(horse).map(Object.class::cast).orElse(""));
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
