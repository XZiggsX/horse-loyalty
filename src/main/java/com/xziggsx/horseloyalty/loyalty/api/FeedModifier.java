package com.xziggsx.horseloyalty.loyalty.api;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Changes the loyalty points of a counted feed, such as hunger doubling them for a favorite food.
 * Gets the points so far and returns the new ones, so modifiers stack. A negative result takes
 * points from the feeder instead (never below level 1). Add one with
 * {@code context.contribute(FeedModifiers.ALL, modifier)}. Asked on the server only.
 */
@FunctionalInterface
public interface FeedModifier {
	/** The points this feed is worth, from {@code points}. */
	int feedPoints(AbstractHorse horse, Player player, ItemStack stack, int points);
}
