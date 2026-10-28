package com.xziggsx.horseloyalty.genetics.trait;

import com.xziggsx.horseloyalty.genetics.core.Traits;

/** The traits a horse has, registered in {@link Traits} when this class loads. */
public final class HorseTraits {
	public static final CoatTrait COAT = Traits.register(new CoatTrait());
	public static final MarkingTrait MARKING = Traits.register(new MarkingTrait());
	public static final TemperamentTrait TEMPERAMENT = Traits.register(new TemperamentTrait());

	private HorseTraits() {
	}

	/** Forces the class to load so the traits are registered. */
	public static void init() {
	}
}
