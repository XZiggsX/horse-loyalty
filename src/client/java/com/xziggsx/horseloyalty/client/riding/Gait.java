package com.xziggsx.horseloyalty.client.riding;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import net.minecraft.network.chat.Component;

/**
 * Riding speeds. Tap W to shift up one gait, tap S to shift down one. Pressing S again
 * at a halt and holding it backs the horse up (see {@link #REVERSE_SPEED}).
 * <p>
 * Each gait is a fixed share of the horse's own top speed, set in the config
 * ({@code gaitSpeedPercent}, by default walk 30%, trot 50%, canter 75%, gallop 100%), so the
 * gaits feel clearly different on every horse. The rider's loyalty then scales all of them.
 * <p>
 * Speeds are "effective" movement speeds: the speed value the horse moves with when
 * given a full forward input. A player walking moves at 0.1 speed with a 0.98 input,
 * so 0.098 here is exactly a player's walking pace (about 4.3 blocks/s).
 */
public enum Gait {
	HALT,
	WALK,
	TROT,
	CANTER,
	GALLOP;

	/** Unenchanted player walking pace, about 4.3 blocks/s. */
	public static final double PLAYER_WALK_SPEED = 0.1 * 0.98;
	/** Converts an effective speed to blocks per second on flat ground. */
	public static final double SPEED_TO_BLOCKS_PER_SECOND = 4.317 / PLAYER_WALK_SPEED;
	/** Backing up, about 1.5 blocks/s whatever the horse or loyalty. */
	public static final double REVERSE_SPEED = 1.5 / SPEED_TO_BLOCKS_PER_SECOND;

	/** The next faster gait, or GALLOP if already galloping. */
	public Gait up() {
		return this == GALLOP ? GALLOP : values()[ordinal() + 1];
	}

	/** The next slower gait, or HALT if already halted. */
	public Gait down() {
		return this == HALT ? HALT : values()[ordinal() - 1];
	}

	/**
	 * @param horseSpeed the horse's movement speed attribute
	 * @return the effective speed for this gait before the loyalty cap: its configured
	 *         share of the horse's top speed. Each gait is faster than the one below.
	 */
	public double speed(double horseSpeed) {
		return horseSpeed * 0.98 * HorseLoyaltyConfig.get().gaitSpeedShare(ordinal());
	}

	/** Translated name, from the lang key {@code horseloyalty.gait.NAME}. */
	public Component displayName() {
		return Component.translatable("horseloyalty.gait." + name().toLowerCase());
	}
}
