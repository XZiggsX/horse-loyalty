package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.personality.Personality;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** A horse's personality, written to {@link Personality#DATA}. Every horse with loyalty has one, donkeys and mules too. */
public final class TemperamentBinding implements TraitBinding<Personality> {
	@Override
	public Trait<Personality> trait() {
		return HorseTraits.TEMPERAMENT;
	}

	@Override
	public boolean appliesTo(AbstractHorse horse) {
		return Loyalty.appliesTo(horse);
	}

	@Override
	public Personality current(AbstractHorse horse) {
		return horse.getAttached(Personality.DATA);
	}

	@Override
	public void apply(AbstractHorse horse, Personality value) {
		horse.setAttached(Personality.DATA, value);
	}
}
