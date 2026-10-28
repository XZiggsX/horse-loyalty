package com.xziggsx.horseloyalty.riding.api;

import com.xziggsx.horseloyalty.core.link.Contributions;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Every {@link RideModifier} of the features that are on, folded over riding's own numbers. The
 * list is empty while every modifying feature is off, so riding then uses its base numbers.
 */
public final class RideModifiers {
	/** The modifiers. Add to it with {@code context.contribute(RideModifiers.ALL, modifier)}. */
	public static final Contributions<RideModifier> ALL = new Contributions<>("riding:ride_modifiers");

	private RideModifiers() {
	}

	/** The gait drop chance after every modifier, from riding's base chance. */
	public static double gaitDropChance(AbstractHorse horse, Player rider, double chance) {
		for (RideModifier modifier : ALL.active()) {
			chance = modifier.gaitDropChance(horse, rider, chance);
		}
		return chance;
	}

	/** The ticks between gait drop rolls after every modifier, from riding's base ticks. */
	public static int gaitDropTicks(AbstractHorse horse, Player rider, int ticks) {
		for (RideModifier modifier : ALL.active()) {
			ticks = modifier.gaitDropTicks(horse, rider, ticks);
		}
		return ticks;
	}

	/** The buck chance after every modifier, from riding's base chance (0). */
	public static double buckChance(AbstractHorse horse, Player rider, double chance) {
		for (RideModifier modifier : ALL.active()) {
			chance = modifier.buckChance(horse, rider, chance);
		}
		return chance;
	}

	/** The gallop push hold ticks after every modifier, from riding's base ticks. */
	public static int gallopPushHoldTicks(AbstractHorse horse, Player rider, int ticks) {
		for (RideModifier modifier : ALL.active()) {
			ticks = modifier.gallopPushHoldTicks(horse, rider, ticks);
		}
		return ticks;
	}

	/** The longest bolt any modifier asks for now, or 0. */
	public static int spook(AbstractHorse horse, Player rider) {
		int ticks = 0;
		for (RideModifier modifier : ALL.active()) {
			ticks = Math.max(ticks, modifier.spook(horse, rider));
		}
		return ticks;
	}
}
