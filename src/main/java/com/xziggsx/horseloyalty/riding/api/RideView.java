package com.xziggsx.horseloyalty.riding.api;

import com.xziggsx.horseloyalty.core.link.LinkKey;
import com.xziggsx.horseloyalty.core.link.Links;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * A ridden horse's gait and push, as other features see them: feedback's HUD, hoof sounds and
 * particles, and the dev tools. Provided by riding; on the client, riding's own ride state answers
 * for the horse you ride. While riding is off there is no gait at all (see {@link NeutralRideView}).
 */
public interface RideView {
	LinkKey<RideView> KEY = new LinkKey<>("riding:ride_view", RideView.class, new NeutralRideView());

	/** The view as it is now: the riding feature's, or the neutral one while riding is off. */
	static RideView get() {
		return Links.get(KEY);
	}

	/** The gait the rider reported to the server, synced to every side. Null if nobody with the mod rides the horse. */
	Gait syncedGait(AbstractHorse horse);

	/**
	 * The gait as this side knows it best: on your own client, your own ride's gait (no network
	 * delay); otherwise the synced one. Null if there is none.
	 */
	Gait gait(AbstractHorse horse);

	/** How hard the rider pushes a gallop, from 0 to 1. Only the rider's own client knows; 0 elsewhere. */
	double pushEffort(AbstractHorse horse);
}
