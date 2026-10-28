package com.xziggsx.horseloyalty.genetics.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** Every trait binding, so new traits only need adding here. */
public final class Bindings {
	public static final CoatBinding COAT = new CoatBinding();
	public static final MarkingBinding MARKING = new MarkingBinding();
	public static final TemperamentBinding TEMPERAMENT = new TemperamentBinding();

	public static final List<TraitBinding<?>> ALL = List.of(COAT, MARKING, TEMPERAMENT);

	private Bindings() {
	}

	/** The bindings for traits this horse shows. */
	public static List<TraitBinding<?>> applicable(AbstractHorse horse) {
		List<TraitBinding<?>> result = new ArrayList<>(ALL.size());
		for (TraitBinding<?> binding : ALL) {
			if (binding.appliesTo(horse)) {
				result.add(binding);
			}
		}
		return result;
	}
}
