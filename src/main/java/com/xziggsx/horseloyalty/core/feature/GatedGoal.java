package com.xziggsx.horseloyalty.core.feature;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A feature's goal, wrapped so it never starts or keeps running while the feature is off. Goals
 * are added when a horse is built, whatever the switches say, so a switch can change later.
 */
final class GatedGoal extends Goal {
	private final Goal goal;
	private final BooleanSupplier isOn;

	GatedGoal(Goal goal, BooleanSupplier isOn) {
		this.goal = goal;
		this.isOn = isOn;
		setFlags(goal.getFlags());
	}

	@Override
	public boolean canUse() {
		return isOn.getAsBoolean() && goal.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return isOn.getAsBoolean() && goal.canContinueToUse();
	}

	@Override
	public boolean isInterruptable() {
		return goal.isInterruptable();
	}

	@Override
	public void start() {
		goal.start();
	}

	@Override
	public void stop() {
		goal.stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return goal.requiresUpdateEveryTick();
	}

	@Override
	public void tick() {
		goal.tick();
	}

	@Override
	public String toString() {
		return goal.toString();
	}
}
