package com.xziggsx.horseloyalty.riding.client.sense;

import java.util.List;

/**
 * One scan of the cone ahead of the horse: every sampled column and what it is. Each behaviour asks
 * it about the terrain it cares about. Pure data, so behaviours can be tested on made-up scans.
 *
 * @param samples every sampled column, nearest row first
 * @param depth   how many blocks ahead the cone reached
 */
public record ConeScan(List<Sample> samples, int depth) {
	/**
	 * One sampled column.
	 *
	 * @param lateral blocks to the right of the heading (negative is left)
	 * @param forward blocks ahead along the heading
	 * @param lane    which third of its row: -1 left, 0 middle, 1 right
	 * @param weight  how much it counts, see {@link ConeMath#weight}
	 * @param column  what the column is
	 */
	public record Sample(int lateral, int forward, int lane, double weight, Column column) {
		public Terrain terrain() {
			return column.terrain();
		}
	}

	/** A point in the cone, relative to the horse. */
	public record Point(double lateral, double forward) {
	}

	/** Shares of a terrain's weight per third of the cone, adding up to 1. */
	public record Shares(double left, double middle, double right) {
	}

	/** How many sampled columns are this terrain. */
	public int count(Terrain terrain) {
		int count = 0;
		for (Sample sample : samples) {
			if (sample.terrain() == terrain) {
				count++;
			}
		}
		return count;
	}

	/** Whether every column straight ahead is this terrain (and there is at least one). */
	public boolean aheadAll(Terrain terrain) {
		boolean any = false;
		for (Sample sample : samples) {
			if (sample.lateral() == 0) {
				if (sample.terrain() != terrain) {
					return false;
				}
				any = true;
			}
		}
		return any;
	}

	/** Whether any column straight ahead is this terrain. */
	public boolean aheadAny(Terrain terrain) {
		for (Sample sample : samples) {
			if (sample.lateral() == 0 && sample.terrain() == terrain) {
				return true;
			}
		}
		return false;
	}

	/** The weighted middle of every column of this terrain, or null if there is none. */
	public Point centre(Terrain terrain) {
		return centre(terrain, 1);
	}

	/** The weighted middle of every column of this terrain from row {@code fromRow} on, or null if there is none. */
	public Point centre(Terrain terrain, int fromRow) {
		double total = 0;
		double lateral = 0;
		double forward = 0;
		for (Sample sample : samples) {
			if (sample.terrain() == terrain && sample.forward() >= fromRow) {
				total += sample.weight();
				lateral += sample.weight() * sample.lateral();
				forward += sample.weight() * sample.forward();
			}
		}
		return total == 0 ? null : new Point(lateral / total, forward / total);
	}

	/**
	 * How this terrain's weight splits over the left, middle and right thirds of the cone, from row
	 * {@code fromRow} on. All 0 if there is none there.
	 */
	public Shares shares(Terrain terrain, int fromRow) {
		double left = 0;
		double middle = 0;
		double right = 0;
		for (Sample sample : samples) {
			if (sample.terrain() != terrain || sample.forward() < fromRow) {
				continue;
			}
			switch (sample.lane()) {
				case -1 -> left += sample.weight();
				case 1 -> right += sample.weight();
				default -> middle += sample.weight();
			}
		}
		double total = left + middle + right;
		return total == 0 ? new Shares(0, 0, 0) : new Shares(left / total, middle / total, right / total);
	}
}
