package com.xziggsx.horseloyalty.horseinfo;

import net.minecraft.network.chat.Component;

/**
 * One part of a {@link HorseProfile}: either its value, or locked with a reason to show
 * instead (such as "reach loyalty 3"). Screens draw whichever it is, without knowing why.
 */
public sealed interface Shown<T> {
	/** The part can be shown. */
	record Visible<T>(T value) implements Shown<T> {
	}

	/** The part is hidden; {@code reason} says how to see it. */
	record Locked<T>(Component reason) implements Shown<T> {
	}

	static <T> Shown<T> visible(T value) {
		return new Visible<>(value);
	}

	static <T> Shown<T> locked(Component reason) {
		return new Locked<>(reason);
	}
}
