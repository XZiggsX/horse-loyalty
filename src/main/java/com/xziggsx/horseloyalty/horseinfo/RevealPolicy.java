package com.xziggsx.horseloyalty.horseinfo;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Decides which parts of the horse info screen a player may see. The one place to gate parts
 * by loyalty later; see {@link RevealPolicies}.
 */
@FunctionalInterface
public interface RevealPolicy {
	/** Empty if the player may see this part, or the reason it's locked. */
	Optional<Component> lockReason(InfoTopic topic, AbstractHorse horse, Player player);
}
