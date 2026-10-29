package com.xziggsx.horseloyalty.riding.client.sense;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks the {@link SenseCone} ahead of the horse and asks a {@link TerrainProbe} about each column.
 * Uses no game objects, so it's tested with a fake grid (see {@code ConeScannerTest}).
 */
public final class ConeScanner {
	private ConeScanner() {
	}

	/**
	 * Scans the cone in front of a horse at ({@code x}, {@code z}) facing {@code yaw} (Minecraft's
	 * yaw: 0 faces +Z, 90 faces -X), standing in block row {@code feetY}.
	 */
	public static ConeScan scan(TerrainProbe probe, double x, double z, int feetY, float yaw, SenseCone cone) {
		double radians = Math.toRadians(yaw);
		double forwardX = -Math.sin(radians);
		double forwardZ = Math.cos(radians);
		// The horse's right-hand side: the heading turned a quarter clockwise.
		double rightX = -forwardZ;
		double rightZ = forwardX;

		List<ConeScan.Sample> samples = new ArrayList<>();
		// At an angle two cone positions can round to the same block; each block is read once.
		Set<Long> seen = new HashSet<>();
		for (int row = 1; row <= cone.depth(); row++) {
			if (!ConeMath.sampled(row)) {
				continue;
			}
			double halfWidth = ConeMath.halfWidth(row, cone.depth(), cone.farHalfWidth());
			int span = (int) Math.floor(halfWidth + 1e-9);
			for (int lateral = -span; lateral <= span; lateral++) {
				int blockX = (int) Math.floor(x + forwardX * row + rightX * lateral);
				int blockZ = (int) Math.floor(z + forwardZ * row + rightZ * lateral);
				if (!seen.add(((long) blockX << 32) ^ (blockZ & 0xFFFFFFFFL))) {
					continue;
				}
				samples.add(new ConeScan.Sample(lateral, row, ConeMath.lane(lateral, halfWidth),
						ConeMath.weight(lateral, row), probe.probe(blockX, blockZ, feetY)));
			}
		}
		return new ConeScan(List.copyOf(samples), cone.depth());
	}
}
