package com.xziggsx.horseloyalty.genetics.core;

import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

/** The randomness genetics uses, so the game can pass its own and tests a seeded one. */
public interface Dice {
	/** A random int from 0 (inclusive) to bound (exclusive). */
	int nextInt(int bound);

	/** A random double from 0 (inclusive) to 1 (exclusive). */
	double nextDouble();

	/** Dice from any source of randomness, such as {@code Dice.of(random::nextInt, random::nextDouble)}. */
	static Dice of(IntUnaryOperator nextInt, DoubleSupplier nextDouble) {
		return new Dice() {
			@Override
			public int nextInt(int bound) {
				return nextInt.applyAsInt(bound);
			}

			@Override
			public double nextDouble() {
				return nextDouble.getAsDouble();
			}
		};
	}
}
