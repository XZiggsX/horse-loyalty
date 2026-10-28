package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.mixin.HorseInvoker;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Markings;

/**
 * A horse's white markings. Horses only, like {@link CoatBinding}.
 * Vanilla only sets coat and markings together, so this keeps the current coat.
 */
public final class MarkingBinding implements TraitBinding<Markings> {
	@Override
	public Trait<Markings> trait() {
		return HorseTraits.MARKING;
	}

	@Override
	public boolean appliesTo(AbstractHorse horse) {
		return horse instanceof Horse;
	}

	@Override
	public Markings current(AbstractHorse horse) {
		return ((Horse) horse).getMarkings();
	}

	@Override
	public void apply(AbstractHorse horse, Markings value) {
		((HorseInvoker) horse).horseloyalty$setVariantAndMarkings(((Horse) horse).getVariant(), value);
	}
}
