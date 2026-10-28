package com.xziggsx.horseloyalty.genetics;

import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.view.GeneticsView;
import com.xziggsx.horseloyalty.genetics.view.ShownTrait;
import com.xziggsx.horseloyalty.genetics.world.Lineage;
import java.util.Optional;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Genetics' section in the dev HUD and {@code /hl info}: what the horse shows, its genes one locus
 * per row, and whether its parents are known. Genes and lineage are synced, so this works in
 * multiplayer too.
 */
final class GeneticsDebug implements DebugSource {
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		DebugSection section = new DebugSection("Genetics");
		for (ShownTrait<?> shown : GeneticsView.shownTraits(horse)) {
			String id = shown.trait().id();
			section.add(Character.toUpperCase(id.charAt(0)) + id.substring(1),
					shown.current() instanceof Enum<?> value ? Locus.name(value) : "none");
		}
		Optional<Genome> genome = GeneticsView.genome(horse);
		if (genome.isEmpty()) {
			section.add("Genes", "none yet", DebugTone.DIM);
		} else {
			genome.get().pairs().forEach((locus, pair) -> section.add(locus.id(),
					Locus.name(pair.first()) + " / " + Locus.name(pair.second()), DebugTone.DIM));
		}
		Optional<Lineage> lineage = GeneticsView.lineage(horse);
		if (lineage.isEmpty() || !lineage.get().hasParents()) {
			section.add("Parents", "not recorded (older horse)", DebugTone.DIM);
		} else {
			Lineage parents = lineage.get();
			boolean anyNamed = parents.nameA().isPresent() || parents.nameB().isPresent();
			String names = parents.nameA().orElse("unnamed") + " x " + parents.nameB().orElse("unnamed");
			section.add("Parents", "known" + (anyNamed ? "  (" + names + ")" : ""));
		}
		return section;
	}
}
