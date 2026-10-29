package com.xziggsx.horseloyalty.riding.client.sense;

import java.util.HashMap;
import java.util.Map;

/**
 * Remembers columns read in the last half second. At a gallop the cone moves about 1.5 blocks per
 * scan, so most of it was read last time and only the new front rows need the level. A block
 * changed within that half second is missed until the entry expires, which is fine for steering.
 */
public final class ColumnCache implements TerrainProbe {
	/** How long a column stays valid, in ticks. */
	static final int KEEP_TICKS = 10;
	/** Above this many entries, expired ones are dropped. */
	private static final int TIDY_ABOVE = 1024;

	private record Entry(Column column, long readAt) {
	}

	private final TerrainProbe source;
	private final Map<Long, Entry> entries = new HashMap<>();
	private long now;

	public ColumnCache(TerrainProbe source) {
		this.source = source;
	}

	/** Sets the time (game ticks) that new reads are stamped with and old ones are judged by. */
	public void setTime(long now) {
		this.now = now;
		if (entries.size() > TIDY_ABOVE) {
			entries.values().removeIf(entry -> now - entry.readAt() >= KEEP_TICKS);
		}
	}

	/** Forgets every column. */
	public void clear() {
		entries.clear();
	}

	@Override
	public Column probe(int x, int z, int feetY) {
		long key = key(x, feetY, z);
		Entry entry = entries.get(key);
		if (entry != null && now - entry.readAt() < KEEP_TICKS) {
			return entry.column();
		}
		Column column = source.probe(x, z, feetY);
		// An unloaded column may load any moment, so it's never kept.
		if (column.terrain() != Terrain.UNLOADED) {
			entries.put(key, new Entry(column, now));
		}
		return column;
	}

	/** Packs a block position into a long, like BlockPos.asLong, without needing the game. */
	private static long key(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
	}
}
