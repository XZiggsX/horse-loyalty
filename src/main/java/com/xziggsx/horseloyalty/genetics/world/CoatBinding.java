package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.mixin.HorseInvoker;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Variant;

/**
 * A horse's coat colour. Horses only: donkeys, mules and the undead horses have one look.
 * Vanilla only sets coat and markings together, so this keeps the current markings.
 */
public final class CoatBinding implements TraitBinding<Variant> {
	@Override
	public Trait<Variant> trait() {
		return HorseTraits.COAT;
	}

	@Override
	public boolean appliesTo(AbstractHorse horse) {
		return horse instanceof Horse;
	}

	@Override
	public Variant current(AbstractHorse horse) {
		return ((Horse) horse).getVariant();
	}

	@Override
	public void apply(AbstractHorse horse, Variant value) {
		((HorseInvoker) horse).horseloyalty$setVariantAndMarkings(value, ((Horse) horse).getMarkings());
	}
}
