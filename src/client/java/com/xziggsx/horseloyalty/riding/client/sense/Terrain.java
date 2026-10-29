package com.xziggsx.horseloyalty.riding.client.sense;

/**
 * What one column of blocks in front of the horse is, as the horse sees it. Read from 2 blocks above
 * the horse's feet down to 3 below (see {@link TerrainProbe}).
 */
public enum Terrain {
	/** Ground the horse can walk on, with its top block in the {@code horseloyalty:path_blocks} tag. */
	PATH,
	/** Any other ground the horse can walk on, including a 1-block step up or a drop of up to 3. */
	GROUND,
	/** In the horse's way: a block at its chest (1 above its feet), or a step it can't climb or has no headroom for. */
	WALL,
	/** Nothing solid down to 3 blocks below the horse's feet. */
	DROP,
	/** In a chunk this client hasn't loaded, so unknown. */
	UNLOADED
}
