package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.genetics.core.Trait;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Connects a {@link Trait} to the game: which horses show it, what one shows now, and how to
 * make it show a value. The bindings never read genes; {@link HorseGenetics} does that.
 *
 * @param <T> what the trait shows
 */
public interface TraitBinding<T> {
	Trait<T> trait();

	/** Whether this kind of horse shows the trait at all (donkeys have no coat genes, for one). */
	boolean appliesTo(AbstractHorse horse);

	/** What the horse shows now, or null if it has no value yet. Server side. */
	T current(AbstractHorse horse);

	/** Makes the horse show this value. Server side. */
	void apply(AbstractHorse horse, T value);
}
