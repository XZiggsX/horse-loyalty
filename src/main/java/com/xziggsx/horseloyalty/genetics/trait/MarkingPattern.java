package com.xziggsx.horseloyalty.genetics.trait;

import java.util.Locale;
import net.minecraft.world.entity.animal.equine.Markings;

/**
 * The real name of a horse's white pattern, worked out from its genes (see {@link MarkingTrait#pattern}).
 * Names are in the lang file under {@code horseloyalty.marking.<id>}.
 */
public enum MarkingPattern {
	TOBIANO(Markings.WHITE_FIELD),
	FEWSPOT(Markings.BLACK_DOTS),
	LEOPARD(Markings.WHITE_DOTS),
	WHITE_SOCKS(Markings.WHITE),
	NONE(Markings.NONE);

	private final Markings texture;

	MarkingPattern(Markings texture) {
		this.texture = texture;
	}

	/** The vanilla markings this pattern is shown with. */
	public Markings texture() {
		return texture;
	}

	/** The id used in lang keys, such as "white_socks". */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
