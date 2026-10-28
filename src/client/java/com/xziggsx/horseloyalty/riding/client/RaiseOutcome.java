package com.xziggsx.horseloyalty.riding.client;

import java.util.Locale;

/** What a horse does when its rider asks for more speed, see {@link GaitRaise}. */
public enum RaiseOutcome {
	/** Goes faster as asked. */
	OK,
	/** Still unsettled from refusing before; nothing happens. */
	UNSETTLED,
	/** Refuses and rears up; the gait stays as it was. */
	PRANCE,
	/** Refuses and throws its rider off. */
	BUCK;

	/** Lang key part for the toast, such as "prance". */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
