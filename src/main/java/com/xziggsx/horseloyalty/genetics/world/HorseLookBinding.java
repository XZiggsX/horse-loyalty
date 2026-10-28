package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Trait;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * A trait a horse shows in its look, such as its coat or markings. Horses only: donkeys, mules
 * and the undead horses have one look.
 *
 * @param <T> what the trait shows
 */
public final class HorseLookBinding<T> implements TraitBinding<T> {
	private final Trait<T> trait;
	private final Function<Horse, T> getter;
	private final BiConsumer<Horse, T> setter;

	public HorseLookBinding(Trait<T> trait, Function<Horse, T> getter, BiConsumer<Horse, T> setter) {
		this.trait = trait;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public Trait<T> trait() {
		return trait;
	}

	@Override
	public boolean appliesTo(AbstractHorse horse) {
		return horse instanceof Horse;
	}

	@Override
	public T current(AbstractHorse horse) {
		return getter.apply((Horse) horse);
	}

	@Override
	public void apply(AbstractHorse horse, T value) {
		setter.accept((Horse) horse, value);
	}
}
