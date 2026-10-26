package com.example.horseloyalty.client.riding;

import com.example.horseloyalty.config.HorseLoyaltyConfig;
import com.example.horseloyalty.loyalty.Loyalty;
import com.example.horseloyalty.personality.Personality;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Gait speed, steering and gait drop numbers for a rider, scaled by their loyalty level.
 * The numbers per level come from the user config, see {@link HorseLoyaltyConfig}.
 * Client side only: riding runs on the rider's client.
 */
public final class Riding {
	private Riding() {
	}

	/**
	 * Highest speed the horse will run for this rider, as a share of its own top speed.
	 * Gaits above the cap run at the cap.
	 */
	public static double topSpeedShare(AbstractHorse horse, Player player) {
		return HorseLoyaltyConfig.get().topSpeedShare(Loyalty.level(horse, player));
	}

	/**
	 * Top turn rate for A and D, in degrees per tick. Like a boat, the turn rate is fixed
	 * for a level, so the faster the gait the wider the circle (radius scales with 1 / rate).
	 */
	public static float turnRate(AbstractHorse horse, Player player) {
		return HorseLoyaltyConfig.get().turnDegreesPerTick(Loyalty.level(horse, player));
	}

	/** Turn rate for a rider that isn't a player. Only players steer horses, so this is a fallback. */
	public static float defaultTurnRate() {
		return HorseLoyaltyConfig.get().turnDegreesPerTick(1);
	}

	/** How much the turn rate may change per tick, so a turn eases in over the configured ticks. */
	public static float turnEase(float maxRate) {
		return maxRate / HorseLoyaltyConfig.get().turnEaseTicks();
	}

	/**
	 * Effective speed for a gait with this rider, after the loyalty cap.
	 * @param horseSpeed the horse's movement speed attribute
	 */
	public static double gaitSpeed(Gait gait, double horseSpeed, AbstractHorse horse, Player player) {
		double cap = Gait.GALLOP.speed(horseSpeed) * topSpeedShare(horse, player);
		return Math.min(gait.speed(horseSpeed), cap);
	}

	/** Chance per check that the horse drops a gait on its own for this rider. Lazy horses drop more often. */
	public static double gaitDropChance(AbstractHorse horse, Player player) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		double chance = config.gaitDropChance(Loyalty.level(horse, player));
		if (Personality.of(horse) == Personality.LAZY) {
			chance = Math.min(1, chance * config.lazyGaitDropChanceMultiplier());
		}
		return chance;
	}

	/** Ticks of riding without shifting gait before each gait drop roll for this rider. Lazy horses roll sooner. */
	public static int gaitDropTicks(AbstractHorse horse, Player player) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		int ticks = config.gaitDropTicks(Loyalty.level(horse, player));
		if (Personality.of(horse) == Personality.LAZY) {
			ticks = (int) Math.max(20, Math.round(ticks * config.lazyGaitDropTimeMultiplier()));
		}
		return ticks;
	}

	/**
	 * How long a skittish horse bolts for this rider when a creeper comes near, in ticks.
	 * Half as long at loyalty 4; a level 5 horse trusts its rider and doesn't bolt at all.
	 */
	public static int boltTicks(AbstractHorse horse, Player player) {
		int level = Loyalty.level(horse, player);
		int ticks = HorseLoyaltyConfig.get().boltTicks();
		return level >= Loyalty.MAX_LEVEL ? 0 : level == Loyalty.MAX_LEVEL - 1 ? ticks / 2 : ticks;
	}
}
