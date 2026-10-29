package com.xziggsx.horseloyalty.riding.client.sense;

/**
 * The shape of the cone the horse scans ahead of itself: one row of block columns per block of
 * depth, widening from {@link ConeMath#NEAR_HALF_WIDTH} blocks either side up close to
 * {@code farHalfWidth} at the far end.
 *
 * @param depth        how many blocks ahead the cone reaches
 * @param farHalfWidth blocks either side of the heading at the far end
 */
public record SenseCone(int depth, double farHalfWidth) {
	/**
	 * A cone that reaches {@code lookAheadTicks} of travel ahead, kept between
	 * {@link ConeMath#MIN_DEPTH} and {@link ConeMath#MAX_DEPTH} blocks.
	 */
	public static SenseCone of(double blocksPerTick, int lookAheadTicks, double farHalfWidth) {
		return new SenseCone(ConeMath.depth(blocksPerTick, lookAheadTicks), farHalfWidth);
	}
}
