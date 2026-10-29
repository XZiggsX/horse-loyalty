package com.xziggsx.horseloyalty.riding.client.sense;

/**
 * Reads one column of blocks for the {@link ConeScanner}. The real one reads the client's level
 * ({@link LevelTerrainProbe}); tests pass a fake grid, so the cone needs no game to be tested.
 */
@FunctionalInterface
public interface TerrainProbe {
	/** Highest block of a column to read, counted from the horse's feet. Above +1 the horse can't fit. */
	int TOP = 2;
	/** Lowest block of a column to read. Nothing solid down to here is a drop. */
	int BOTTOM = -3;

	/**
	 * The column at block {@code x}, {@code z}, read from {@link #TOP} down to {@link #BOTTOM} around
	 * {@code feetY}, the block the horse stands in.
	 */
	Column probe(int x, int z, int feetY);
}
