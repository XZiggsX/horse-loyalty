package com.xziggsx.horseloyalty.client.horseinfo;

import java.util.Map;

/** Every colour the horse info screen uses, in one place. */
public final class InfoPalette {
	public static final int TEXT = 0xFF404040;
	public static final int DIM = 0xFF7A7A7A;
	public static final int SLOT_SHADOW = 0xFF373737;
	public static final int SLOT_LIGHT = 0xFFFFFFFF;
	public static final int BAR_BACK = 0xFF555555;
	public static final int CHIP_TEXT = 0xFFFFFFFF;
	public static final int CHIP_CARRIED = 0xFF8B8B8B;
	public static final int CHIP_CARRIED_TEXT = 0xFFDDDDDD;
	public static final int SCROLL_TRACK = 0xFF8B8B8B;
	public static final int SCROLL_THUMB = 0xFF555555;
	public static final int WHITE_HAIR = 0xFFF4F4F4;

	private static final int FALLBACK = 0xFF5C7FA6;

	private static final Map<String, Integer> BARS = Map.of(
			"health", 0xFFD64545,
			"speed", 0xFF4FB3D9,
			"jump", 0xFF7FCF5A);

	/** Coat swatches, roughly the colour of the vanilla texture each coat is drawn with. */
	private static final Map<String, Integer> COATS = Map.ofEntries(
			Map.entry("grey", 0xFFA4A4A8),
			Map.entry("cremello", 0xFFF2E6C8),
			Map.entry("perlino", 0xFFE0C384),
			Map.entry("smoky_cream", 0xFFD2B886),
			Map.entry("palomino", 0xFFD8A54A),
			Map.entry("buckskin", 0xFFC89A55),
			Map.entry("brown_buckskin", 0xFFB0864E),
			Map.entry("smoky_black", 0xFF2E2A28),
			Map.entry("chestnut", 0xFF8E4A26),
			Map.entry("bay", 0xFF7A4A2A),
			Map.entry("seal_brown", 0xFF4A2E1E),
			Map.entry("black", 0xFF1E1C1C));

	/** Chip colours for copies that show, by "locus.version". */
	private static final Map<String, Integer> CHIPS = Map.ofEntries(
			Map.entry("red_black.black", 0xFF2B2B2B),
			Map.entry("red_black.red", 0xFFB5532A),
			Map.entry("agouti.bay", 0xFF8A5A2B),
			Map.entry("agouti.seal_brown", 0xFF5A3A22),
			Map.entry("agouti.black", 0xFF2B2B2B),
			Map.entry("cream.cream", 0xFFC09A3A),
			Map.entry("gray.gray", 0xFF7F7F90),
			Map.entry("pinto.pinto", 0xFF5D82AA),
			Map.entry("leopard.leopard", 0xFF7A6A9A),
			Map.entry("socks.socks", 0xFF5D82AA),
			Map.entry("temperament.brave", 0xFFC98A1E),
			Map.entry("temperament.skittish", 0xFF9A4FC0),
			Map.entry("temperament.curious", 0xFF3F9F6B),
			Map.entry("temperament.lazy", 0xFF5C7FA6),
			Map.entry("temperament.steady", 0xFF6F7F6F));

	private InfoPalette() {
	}

	public static int bar(String statId) {
		return BARS.getOrDefault(statId, FALLBACK);
	}

	public static int coat(String coatId) {
		return COATS.getOrDefault(coatId, FALLBACK);
	}

	/** The chip colour for a copy that shows. Copies that don't show use {@link #CHIP_CARRIED}. */
	public static int chip(String locusId, String versionId) {
		return CHIPS.getOrDefault(locusId + "." + versionId, FALLBACK);
	}
}
