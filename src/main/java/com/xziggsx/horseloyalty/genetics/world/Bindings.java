package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.mixin.HorseInvoker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;

/** Every trait binding, so new traits only need adding here. */
public final class Bindings {
	/** The coat. Vanilla only sets coat and markings together, so this keeps the current markings. */
	public static final HorseLookBinding<Variant> COAT = new HorseLookBinding<>(HorseTraits.COAT, Horse::getVariant,
			(horse, variant) -> ((HorseInvoker) horse).horseloyalty$setVariantAndMarkings(variant, horse.getMarkings()));
	/** The white markings, keeping the current coat. */
	public static final HorseLookBinding<Markings> MARKING = new HorseLookBinding<>(HorseTraits.MARKING, Horse::getMarkings,
			(horse, markings) -> ((HorseInvoker) horse).horseloyalty$setVariantAndMarkings(horse.getVariant(), markings));
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
