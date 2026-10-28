package com.xziggsx.horseloyalty.genetics.trait;

import com.xziggsx.horseloyalty.genetics.core.Trait;
import java.util.List;

/** The traits a horse has. */
public final class HorseTraits {
	public static final CoatTrait COAT = new CoatTrait();
	public static final MarkingTrait MARKING = new MarkingTrait();
	public static final TemperamentTrait TEMPERAMENT = new TemperamentTrait();

	/** Every trait, in display order. */
	public static final List<Trait<?>> ALL = List.of(COAT, MARKING, TEMPERAMENT);

	private HorseTraits() {
	}
}
