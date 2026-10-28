package com.xziggsx.horseloyalty.horseinfo;

import com.xziggsx.horseloyalty.horseinfo.api.RevealPolicy;
import java.util.Optional;

/** The available {@link RevealPolicy}s. */
public final class RevealPolicies {
	private static final RevealPolicy EVERYTHING = (topic, horse, player) -> Optional.empty();

	private RevealPolicies() {
	}

	/**
	 * Shows every part to everyone. The current choice; a policy that locks parts until a
	 * loyalty level (from the config) can replace it without touching the screen.
	 */
	public static RevealPolicy everything() {
		return EVERYTHING;
	}
}
