package com.xziggsx.horseloyalty.loyalty.api;

import com.xziggsx.horseloyalty.core.link.LinkKey;
import com.xziggsx.horseloyalty.core.link.Links;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * How much a horse trusts a player: the loyalty level 0 to 5. Provided by loyalty; read by riding,
 * summoning, personality, foals, feedback and horse info. While loyalty is off, every tamed horse
 * is at {@link #MAX_LEVEL} for every rider (see {@link NeutralBond}).
 */
public interface Bond {
	int MAX_LEVEL = 5;

	LinkKey<Bond> KEY = new LinkKey<>("loyalty:bond", Bond.class, new NeutralBond());

	/** The bond as it is now: the loyalty feature's, or the neutral one while loyalty is off. */
	static Bond get() {
		return Links.get(KEY);
	}

	/** Translated level name, such as "Trusting", from the lang key horseloyalty.loyalty.level.N. */
	static Component levelName(int level) {
		return Component.translatable("horseloyalty.loyalty.level." + level);
	}

	/** This player's level with the horse, 0 if untamed. Works on both sides. */
	int level(AbstractHorse horse, Player player);

	/** This player's points with the horse, for a foal's head start. Server side. */
	int points(AbstractHorse horse, Player player);

	/** Gives a player bonus points without a level-up celebration. Server side. */
	void startWith(AbstractHorse horse, Player player, int points);

	/** Takes points from every rider at once, never below level 1. Server side. */
	void lose(AbstractHorse horse, int points);
}
