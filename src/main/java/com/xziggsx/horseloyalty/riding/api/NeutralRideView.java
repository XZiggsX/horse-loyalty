package com.xziggsx.horseloyalty.riding.api;

import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** The {@link RideView} while riding is off: vanilla riding has no gaits. */
final class NeutralRideView implements RideView {
	@Override
	public Gait syncedGait(AbstractHorse horse) {
		return null;
	}

	@Override
	public Gait gait(AbstractHorse horse) {
		return null;
	}

	@Override
	public double pushEffort(AbstractHorse horse) {
		return 0;
	}
}
