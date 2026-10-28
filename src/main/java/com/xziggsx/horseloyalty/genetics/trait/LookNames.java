package com.xziggsx.horseloyalty.genetics.trait;

import java.util.Locale;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;

/**
 * The real names of what a horse looks like, worked out from its genes: its coat colour and its
 * white pattern. Vanilla has fewer textures than names, so several names share one texture.
 */
public final class LookNames {
	private LookNames() {
	}

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
}
