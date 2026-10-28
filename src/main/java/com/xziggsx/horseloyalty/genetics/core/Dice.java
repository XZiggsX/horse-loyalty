package com.xziggsx.horseloyalty.genetics.core;

import java.util.Random;

/** The randomness genetics uses, so the game can pass its own and tests a seeded one. */
public interface Dice {
	/** A random int from 0 (inclusive) to bound (exclusive). */
	int nextInt(int bound);

	/** A random double from 0 (inclusive) to 1 (exclusive). */
	double nextDouble();

	/** Dice backed by a java.util.Random. */
	static Dice of(Random random) {
		return new Dice() {
			@Override
			public int nextInt(int bound) {
				return random.nextInt(bound);
			}

			@Override
			public double nextDouble() {
				return random.nextDouble();
			}
		};
	}
}
