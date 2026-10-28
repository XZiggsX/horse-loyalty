package com.xziggsx.horseloyalty.riding.client;

import com.xziggsx.horseloyalty.loyalty.api.Bond;
import com.xziggsx.horseloyalty.riding.RidingSettings;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.RideModifiers;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Gait speed, steering, gait drop and gallop push numbers for a rider, scaled by their loyalty level
 * (see {@link Bond}) and changed by any {@link RideModifiers}. The numbers per level come from the
 * user config, see {@link RidingSettings}. Client side only: riding runs on the rider's client.
 */
public final class Riding {
	private Riding() {
	}

	/**
	 * How fast the horse will go for this rider, as a share of its normal speed.
	 * Every gait is scaled by it, so a less loyal horse is slower in every gait.
	 */
	public static double topSpeedShare(AbstractHorse horse, Player player) {
		return RidingSettings.get().topSpeedShare(Bond.get().level(horse, player));
	}

	/**
	 * Top turn rate for A and D, in degrees per tick. Like a boat, the turn rate is fixed
	 * for a level, so the faster the gait the wider the circle (radius scales with 1 / rate).
	 */
	public static float turnRate(AbstractHorse horse, Player player) {
		return RidingSettings.get().turnDegreesPerTick(Bond.get().level(horse, player));
	}

	/** Turn rate for a rider that isn't a player. Only players steer horses, so this is a fallback. */
	public static float defaultTurnRate() {
		return RidingSettings.get().turnDegreesPerTick(1);
	}

	/** How much the turn rate may change per tick, so a turn eases in over the configured ticks. */
	public static float turnEase(float maxRate) {
		return maxRate / RidingSettings.get().turnEaseTicks();
	}

	/**
	 * The effective speed for a gait before the loyalty cap: its configured share of the horse's
	 * top speed. Each gait is faster than the one below.
	 * <p>
	 * Speeds are "effective" movement speeds: the speed value the horse moves with when given a
	 * full forward input. A player walking moves at 0.1 speed with a 0.98 input, so 0.098 here is
	 * exactly a player's walking pace (about 4.3 blocks/s).
	 *
	 * @param horseSpeed the horse's movement speed attribute
	 */
	public static double baseSpeed(Gait gait, double horseSpeed) {
		return horseSpeed * 0.98 * RidingSettings.get().gaitSpeedShare(gait);
	}

	/**
	 * Effective speed for a gait with this rider, scaled by their loyalty.
	 * @param horseSpeed the horse's movement speed attribute
	 */
	public static double gaitSpeed(Gait gait, double horseSpeed, AbstractHorse horse, Player player) {
		return baseSpeed(gait, horseSpeed) * topSpeedShare(horse, player);
	}

	/** Chance per check that the horse drops a gait on its own for this rider. */
	public static double gaitDropChance(AbstractHorse horse, Player player) {
		double chance = RidingSettings.get().gaitDropChance(Bond.get().level(horse, player));
		return RideModifiers.gaitDropChance(horse, player, chance);
	}

	/** Ticks of riding without shifting gait before each gait drop roll for this rider. */
	public static int gaitDropTicks(AbstractHorse horse, Player player) {
		int ticks = RidingSettings.get().gaitDropTicks(Bond.get().level(horse, player));
		return RideModifiers.gaitDropTicks(horse, player, ticks);
	}

	/**
	 * How long a gallop push holds after the rider's last W tap, in ticks. Each tap restarts it;
	 * when it runs out the extra speed fades back to a normal gallop. Ride modifiers can change it,
	 * such as a more loyal horse or a stamina system keeping up the effort longer.
	 */
	public static int gallopPushTicks(AbstractHorse horse, Player player) {
		int ticks = RidingSettings.get().gallopPushHoldTicks();
		return Math.max(1, RideModifiers.gallopPushHoldTicks(horse, player, ticks));
	}

	/** How long the horse bolts for if something spooks it now, in ticks, or 0. See {@link RideModifiers#spook}. */
	public static int spookTicks(AbstractHorse horse, Player player) {
		return RideModifiers.spook(horse, player);
	}
}
