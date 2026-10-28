package com.xziggsx.horseloyalty.genetics.view;

import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.world.Bindings;
import com.xziggsx.horseloyalty.genetics.world.GeneticsData;
import com.xziggsx.horseloyalty.genetics.world.Lineage;
import com.xziggsx.horseloyalty.genetics.world.TraitBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Read-only access to a horse's genetics, for the dev tools and the horse info screen.
 * Works on both sides, since the data is synced to clients. Never changes anything.
 */
public final class GeneticsView {
	private GeneticsView() {
	}

	/** The horse's genes, if it has any yet. */
	public static Optional<Genome> genome(AbstractHorse horse) {
		return Optional.ofNullable(horse.getAttached(GeneticsData.GENOME));
	}

	/** The horse's parents' genes and names, if known. */
	public static Optional<Lineage> lineage(AbstractHorse horse) {
		return Optional.ofNullable(horse.getAttached(GeneticsData.LINEAGE));
	}

	/** The traits this kind of horse shows (donkeys and mules: temperament only), with what it shows now. */
	public static List<ShownTrait<?>> shownTraits(AbstractHorse horse) {
		List<ShownTrait<?>> shown = new ArrayList<>();
		for (TraitBinding<?> binding : Bindings.applicable(horse)) {
			shown.add(shownTrait(binding, horse));
		}
		return shown;
	}

	private static <T> ShownTrait<T> shownTrait(TraitBinding<T> binding, AbstractHorse horse) {
		return new ShownTrait<>(binding.trait(), binding.current(horse));
	}
}
