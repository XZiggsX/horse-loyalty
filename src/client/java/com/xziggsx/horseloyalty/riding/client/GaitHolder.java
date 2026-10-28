package com.xziggsx.horseloyalty.riding.client;

import com.xziggsx.horseloyalty.riding.api.Gait;

/**
 * Implemented on every horse by {@code AbstractHorseRidingMixin}, so other client code
 * (the dev panel) can read the riding state. Cast a horse to this to use it.
 */
public interface GaitHolder {
	Gait horseloyalty$getGait();

	/** Ticks the horse has been moving since the rider last shifted gait (or the last drop roll). */
	int horseloyalty$getGaitDropTimer();

	/** The horse's gallop push, to read how hard it's pushed. */
	GallopPush horseloyalty$getGallopPush();

	/** Ticks until the horse will go faster again after refusing, 0 if it will now. */
	int horseloyalty$getRaiseCooldown();
}
