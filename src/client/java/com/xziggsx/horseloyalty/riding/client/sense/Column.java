package com.xziggsx.horseloyalty.riding.client.sense;

/**
 * One column of blocks as a {@link TerrainProbe} read it.
 *
 * @param terrain what the column is
 * @param height  the highest solid block, counted from the horse's feet (0 is the block the horse
 *                stands in, so -1 is level ground). 0 for a drop or an unloaded column.
 */
public record Column(Terrain terrain, int height) {
	/** A column in a chunk that isn't loaded. */
	public static final Column UNLOADED = new Column(Terrain.UNLOADED, 0);
	/** A column with nothing solid down to 3 below the feet. */
	public static final Column DROP = new Column(Terrain.DROP, 0);
}
