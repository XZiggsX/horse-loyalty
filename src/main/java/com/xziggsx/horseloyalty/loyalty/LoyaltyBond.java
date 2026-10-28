package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.loyalty.api.Bond;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** The loyalty feature's {@link Bond}: the levels and points kept by {@link Loyalty}. */
final class LoyaltyBond implements Bond {
	@Override
	public int level(AbstractHorse horse, Player player) {
		return Loyalty.level(horse, player);
	}

	@Override
	public int points(AbstractHorse horse, Player player) {
		return Loyalty.get(horse, player).points();
	}

	@Override
	public void startWith(AbstractHorse horse, Player player, int points) {
		Loyalty.startWith(horse, player, points);
	}

	@Override
	public void lose(AbstractHorse horse, int points) {
		Loyalty.loseLoyalty(horse, points);
	}
}
