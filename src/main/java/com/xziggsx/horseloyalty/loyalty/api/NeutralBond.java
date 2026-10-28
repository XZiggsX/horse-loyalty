package com.xziggsx.horseloyalty.loyalty.api;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** The {@link Bond} while loyalty is off: a tamed horse is at MAX_LEVEL for everyone, 0 if untamed. */
final class NeutralBond implements Bond {
	@Override
	public int level(AbstractHorse horse, Player player) {
		return horse.isTamed() ? MAX_LEVEL : 0;
	}

	@Override
	public int points(AbstractHorse horse, Player player) {
		return 0;
	}

	@Override
	public void startWith(AbstractHorse horse, Player player, int points) {
	}

	@Override
	public void lose(AbstractHorse horse, int points) {
	}
}
