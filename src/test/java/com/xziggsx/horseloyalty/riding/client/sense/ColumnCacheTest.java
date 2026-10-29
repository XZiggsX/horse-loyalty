package com.xziggsx.horseloyalty.riding.client.sense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColumnCacheTest {
	private int reads;
	private Column answer = new Column(Terrain.PATH, -1);

	private final ColumnCache cache = new ColumnCache((x, z, feetY) -> {
		reads++;
		return answer;
	});

	@Test
	void keepsColumnsForAWhile() {
		cache.setTime(100);
		cache.probe(1, 2, 64);
		cache.setTime(100 + ColumnCache.KEEP_TICKS - 1);
		cache.probe(1, 2, 64);
		assertEquals(1, reads);
		cache.setTime(100 + ColumnCache.KEEP_TICKS);
		cache.probe(1, 2, 64);
		assertEquals(2, reads);
	}

	@Test
	void columnsAreKeptPerPositionAndHeight() {
		cache.setTime(0);
		cache.probe(1, 2, 64);
		cache.probe(1, 2, 65);
		cache.probe(-1, 2, 64);
		cache.probe(1, -2, 64);
		assertEquals(4, reads);
	}

	@Test
	void unloadedColumnsAreNeverKept() {
		answer = Column.UNLOADED;
		cache.setTime(0);
		cache.probe(1, 2, 64);
		cache.probe(1, 2, 64);
		assertEquals(2, reads);
	}

	@Test
	void clearForgetsEverything() {
		cache.setTime(0);
		cache.probe(1, 2, 64);
		cache.clear();
		cache.probe(1, 2, 64);
		assertEquals(2, reads);
	}
}
