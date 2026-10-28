package com.xziggsx.horseloyalty.client.riding;

import com.xziggsx.horseloyalty.riding.GaitSync;
import com.xziggsx.horseloyalty.riding.api.Gait;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/** A ridden horse's gait as this client knows it, for visuals. */
public final class ClientGait {
	private ClientGait() {
	}

	/**
	 * The horse's gait: our own if we're riding it (no network delay), otherwise the gait its
	 * rider sent the server (see {@link GaitSync}). HALT for a horse nobody with the mod rides.
	 */
	public static Gait of(AbstractHorse horse) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null && horse.getControllingPassenger() == minecraft.player) {
			return ((GaitHolder) horse).horseloyalty$getGait();
		}
		Gait gait = GaitSync.gait(horse);
		return gait == null ? Gait.HALT : gait;
	}
}
