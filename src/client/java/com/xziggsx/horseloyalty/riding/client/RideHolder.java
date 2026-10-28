package com.xziggsx.horseloyalty.riding.client;

/**
 * Implemented on every horse by {@code HorseRidingMixin}, so riding's client code (the client ride
 * view and riding's dev section) can reach the horse's ride. Cast a horse to this to use it.
 */
public interface RideHolder {
	/** The horse's ride state on this client. */
	RideControl horseloyalty$ride();
}
