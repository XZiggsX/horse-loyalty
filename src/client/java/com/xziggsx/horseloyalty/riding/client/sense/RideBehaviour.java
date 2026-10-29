package com.xziggsx.horseloyalty.riding.client.sense;

/**
 * Something the horse does on its own based on what it senses ahead, such as following a path.
 * {@link Senses} scans once and asks each behaviour in turn; the first one with a turn target steers.
 * The rider's own steering always comes first (see {@code RideMath.turnTarget}).
 */
public interface RideBehaviour {
	/** Whether this behaviour needs a fresh scan of the cone this tick. No behaviour wanting one means no scan. */
	boolean wantsScan(SenseState state);

	/**
	 * Runs once per riding tick. Returns the turn rate to aim for in degrees per tick (positive turns
	 * right), or NaN to leave steering alone.
	 *
	 * @param scan the latest scan, or null if there is none
	 */
	float tick(SenseState state, ConeScan scan);

	/** Whether this behaviour is in charge right now. */
	boolean active();

	/** Starts over, at the start of a ride. */
	void reset();
}
