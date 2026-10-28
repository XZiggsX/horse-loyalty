package com.xziggsx.horseloyalty.genetics.trait;

import java.util.Locale;
import net.minecraft.world.entity.animal.equine.Variant;

/**
 * The real name of a horse's coat colour, worked out from its genes (see {@link CoatTrait#colour}).
 * Vanilla only has seven coat textures, so several colours share one: palomino, buckskin and
 * brown buckskin all look creamy. Only cremello uses the white texture: perlino and smoky cream
 * are cream-bodied with darker points, so creamy is the closer match. Names are in the lang file
 * under {@code horseloyalty.coat.<id>}.
 */
public enum CoatColour {
	GREY(Variant.GRAY),
	CREMELLO(Variant.WHITE),
	PERLINO(Variant.CREAMY),
	SMOKY_CREAM(Variant.CREAMY),
	PALOMINO(Variant.CREAMY),
	BUCKSKIN(Variant.CREAMY),
	BROWN_BUCKSKIN(Variant.CREAMY),
	SMOKY_BLACK(Variant.BLACK),
	CHESTNUT(Variant.CHESTNUT),
	BAY(Variant.BROWN),
	SEAL_BROWN(Variant.DARK_BROWN),
	BLACK(Variant.BLACK);

	private final Variant texture;

	CoatColour(Variant texture) {
		this.texture = texture;
	}

	/** The vanilla coat this colour is shown with. */
	public Variant texture() {
		return texture;
	}

	/** The id used in lang keys, such as "smoky_black". */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
