package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.personality.api.Personality;
import com.xziggsx.horseloyalty.personality.api.PersonalityView;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * A horse's personality, read and written through {@link PersonalityView}. Every horse in scope
 * has one, donkeys and mules too. While personality is off nothing is read or written, so the
 * temperament genes are still inherited but not expressed.
 */
public final class TemperamentBinding implements TraitBinding<Personality> {
	@Override
	public Trait<Personality> trait() {
		return HorseTraits.TEMPERAMENT;
	}

	@Override
	public boolean appliesTo(AbstractHorse horse) {
		return HorseScope.inScope(horse);
	}

	@Override
	public Personality current(AbstractHorse horse) {
		return PersonalityView.get().stored(horse);
	}

	@Override
	public void apply(AbstractHorse horse, Personality value) {
		PersonalityView.get().set(horse, value);
	}
}
