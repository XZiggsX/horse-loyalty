package com.example.horseloyalty.client.riding;

/**
 * Implemented on every horse by {@code AbstractHorseRidingMixin}, so other client code
 * (the dev panel) can read the riding state. Cast a horse to this to use it.
 */
public interface GaitHolder {
	Gait horseloyalty$getGait();

	/** Ticks the horse has been moving since the rider last shifted gait (or the last drop roll). */
	int horseloyalty$getGaitDropTimer();
}
