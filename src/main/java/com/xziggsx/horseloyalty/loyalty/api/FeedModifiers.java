package com.xziggsx.horseloyalty.loyalty.api;

import com.xziggsx.horseloyalty.core.link.Contributions;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Every {@link FeedModifier} of the features that are on, folded over loyalty's points per feed. */
public final class FeedModifiers {
	/** The modifiers. Add to it with {@code context.contribute(FeedModifiers.ALL, modifier)}. */
	public static final Contributions<FeedModifier> ALL = new Contributions<>("loyalty:feed_modifiers");

	private FeedModifiers() {
	}

	/** The points of a counted feed after every modifier, from loyalty's base points. */
	public static int feedPoints(AbstractHorse horse, Player player, ItemStack stack, int points) {
		for (FeedModifier modifier : ALL.active()) {
			points = modifier.feedPoints(horse, player, stack, points);
		}
		return points;
	}
}
