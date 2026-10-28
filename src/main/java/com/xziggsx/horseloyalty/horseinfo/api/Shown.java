package com.xziggsx.horseloyalty.horseinfo.api;

import net.minecraft.network.chat.Component;

/**
 * One part of a {@link HorseProfile}: its value, locked with a reason to show instead (such as
 * "reach loyalty 3"), or off because the feature behind it is off. Screens draw whichever it is,
 * without knowing why, and skip parts that are off.
 */
public sealed interface Shown<T> {
	/** The part can be shown. */
	record Visible<T>(T value) implements Shown<T> {
	}

	/** The part is hidden; {@code reason} says how to see it. */
	record Locked<T>(Component reason) implements Shown<T> {
	}

	/** The feature behind this part is off, so there is nothing to show. */
	record Off<T>() implements Shown<T> {
	}

	static <T> Shown<T> visible(T value) {
		return new Visible<>(value);
	}

	static <T> Shown<T> locked(Component reason) {
		return new Locked<>(reason);
	}

	static <T> Shown<T> off() {
		return new Off<>();
	}
}
