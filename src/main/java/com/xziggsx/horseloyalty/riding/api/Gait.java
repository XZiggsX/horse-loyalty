package com.xziggsx.horseloyalty.riding.api;

import com.xziggsx.horseloyalty.core.HorseUnits;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * A ridden horse's gait. Tap W to shift up one gait, tap S to shift down one. Pressing S again at
 * a halt and holding it backs the horse up (see {@link #REVERSE_SPEED}). Each gait's speed is a
 * share of the horse's top speed, set in the riding config. The rider's client decides the gait and
 * reports it, so every side can read it (see {@code GaitSync}).
 */
public enum Gait {
	HALT,
	WALK,
	TROT,
	CANTER,
	GALLOP;

	private static final Gait[] VALUES = values();

	/** Backing up, about 1.5 blocks/s whatever the horse or loyalty. */
	public static final double REVERSE_SPEED = 1.5 / HorseUnits.SPEED_TO_BLOCKS_PER_SECOND;

	/** The next faster gait, or GALLOP if already galloping. */
	public Gait up() {
		return this == GALLOP ? GALLOP : VALUES[ordinal() + 1];
	}

	/** The next slower gait, or HALT if already halted. */
	public Gait down() {
		return this == HALT ? HALT : VALUES[ordinal() - 1];
	}

	/** Whether this gait is at least as fast as {@code other}. */
	public boolean atLeast(Gait other) {
		return ordinal() >= other.ordinal();
	}

	/** The id used in the config and lang keys, such as "trot". */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Translated name, from the lang key {@code horseloyalty.gait.NAME}. */
	public Component displayName() {
		return Component.translatable("horseloyalty.gait." + id());
	}

	/** The gait with this index (0 halt to 4 gallop), clamped to the known gaits. */
	public static Gait byIndex(int index) {
		return VALUES[Math.clamp(index, 0, VALUES.length - 1)];
	}
}
