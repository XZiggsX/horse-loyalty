package com.xziggsx.horseloyalty.riding.api;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Changes riding for one horse and rider. Every method gets riding's value so far and returns the
 * new one, so modifiers stack. The defaults change nothing; implement only what you need. Add one
 * with {@code context.contribute(RideModifiers.ALL, modifier)}. Riding runs on the rider's client,
 * so modifiers are asked there.
 */
public interface RideModifier {
	/** Chance (0.1 = 10%) per check that the horse drops a gait on its own. */
	default double gaitDropChance(AbstractHorse horse, Player rider, double chance) {
		return chance;
	}

	/** Ticks of riding without shifting gait between gait drop rolls. */
	default int gaitDropTicks(AbstractHorse horse, Player rider, int ticks) {
		return ticks;
	}

	/** Chance (0.35 = 35%) that a horse refusing to go faster throws its rider instead of prancing. */
	default double buckChance(AbstractHorse horse, Player rider, double chance) {
		return chance;
	}

	/** Ticks a gallop push holds after the rider's last W tap. */
	default int gallopPushHoldTicks(AbstractHorse horse, Player rider, int ticks) {
		return ticks;
	}

	/** Ticks the horse bolts for now, or 0. Checked twice a second while ridden. */
	default int spook(AbstractHorse horse, Player rider) {
		return 0;
	}
}
