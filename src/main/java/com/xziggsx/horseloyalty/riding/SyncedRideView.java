package com.xziggsx.horseloyalty.riding;

import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.RideView;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** The common {@link RideView}: only the synced gait. The riding client replaces it with one that knows your own ride. */
public class SyncedRideView implements RideView {
	@Override
	public Gait syncedGait(AbstractHorse horse) {
		return GaitSync.gait(horse);
	}

	@Override
	public Gait gait(AbstractHorse horse) {
		return GaitSync.gait(horse);
	}

	@Override
	public double pushEffort(AbstractHorse horse) {
		return 0;
	}
}
