package com.xziggsx.horseloyalty.riding.client;

import com.xziggsx.horseloyalty.riding.GaitSync;
import com.xziggsx.horseloyalty.riding.SyncedRideView;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.RideView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * The client's {@link RideView}: for the horse you ride, your own ride state (no network delay);
 * for any other horse, the gait its rider sent the server (see {@link GaitSync}).
 */
final class ClientRideView extends SyncedRideView {
	@Override
	public Gait gait(AbstractHorse horse) {
		return isOwnRide(horse) ? ((GaitHolder) horse).horseloyalty$getGait() : GaitSync.gait(horse);
	}

	@Override
	public double pushEffort(AbstractHorse horse) {
		return isOwnRide(horse) ? ((GaitHolder) horse).horseloyalty$getGallopPush().effort() : 0;
	}

	private static boolean isOwnRide(AbstractHorse horse) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null && horse.getControllingPassenger() == minecraft.player;
	}
}
