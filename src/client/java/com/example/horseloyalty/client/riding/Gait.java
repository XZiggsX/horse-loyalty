package com.example.horseloyalty.client.riding;

import net.minecraft.network.chat.Component;

/**
 * Riding speeds. Tap W to shift up one gait, tap S to shift down one.
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
	/** Player sprinting pace (walk + 30%), about 5.6 blocks/s. */
	public static final double PLAYER_SPRINT_SPEED = 0.13 * 0.98;
	/** Canter is this share of the horse's own top speed. */
	public static final double CANTER_SHARE = 0.75;
	/** Converts an effective speed to blocks per second on flat ground. */
	public static final double SPEED_TO_BLOCKS_PER_SECOND = 4.317 / PLAYER_WALK_SPEED;

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
	 * @return the effective speed for this gait. Each gait is at least as fast as the
	 *         one below, so a slow horse's canter never drops under a sprint.
	 */
	public double speed(double horseSpeed) {
		double horseTop = horseSpeed * 0.98;
		return switch (this) {
			case HALT -> 0;
			case WALK -> PLAYER_WALK_SPEED;
			case TROT -> PLAYER_SPRINT_SPEED;
			case CANTER -> Math.max(horseTop * CANTER_SHARE, PLAYER_SPRINT_SPEED);
			case GALLOP -> Math.max(horseTop, Math.max(horseTop * CANTER_SHARE, PLAYER_SPRINT_SPEED));
		};
	}

	/** Translated name, from the lang key {@code horseloyalty.gait.NAME}. */
	public Component displayName() {
		return Component.translatable("horseloyalty.gait." + name().toLowerCase());
	}
}
