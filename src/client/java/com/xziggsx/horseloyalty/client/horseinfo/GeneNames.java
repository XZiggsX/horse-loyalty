package com.xziggsx.horseloyalty.client.horseinfo;

import net.minecraft.network.chat.Component;

/**
 * Names for genes, their versions, coats and patterns, from the lang file. Proper genetic
 * names for now; a plain-name style for accessibility can be added as a second set of lang
 * keys and picked here.
 */
public final class GeneNames {
	/** Which set of names to use. */
	public enum NamingStyle {
		/** Proper names, such as "Extension" and "Seal brown". */
		PROPER("horseloyalty.gene.");

		private final String prefix;

		NamingStyle(String prefix) {
			this.prefix = prefix;
		}
	}

	private static NamingStyle style = NamingStyle.PROPER;

	private GeneNames() {
	}

	/** A gene's name, such as "Agouti". */
	public static Component locus(String locusId) {
		return Component.translatable(style.prefix + locusId);
	}

	/** A version's name, such as "Seal brown". */
	public static Component version(String locusId, String versionId) {
		return Component.translatable(style.prefix + locusId + "." + versionId);
	}

	/** A real name from a named trait, such as "Buckskin" for coat or "Tobiano" for marking. */
	public static Component traitValue(String traitId, String valueId) {
		return Component.translatable("horseloyalty." + traitId + "." + valueId);
	}
}
