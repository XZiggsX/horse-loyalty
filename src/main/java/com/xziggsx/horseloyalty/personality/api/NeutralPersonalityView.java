package com.xziggsx.horseloyalty.personality.api;

import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** The {@link PersonalityView} while personality is off: every horse acts steady and nothing is shown or saved. */
final class NeutralPersonalityView implements PersonalityView {
	@Override
	public Personality of(AbstractHorse horse) {
		return Personality.STEADY;
	}

	@Override
	public Optional<Personality> revealedTo(AbstractHorse horse, Player player) {
		return Optional.empty();
	}

	@Override
	public Personality stored(AbstractHorse horse) {
		return null;
	}

	@Override
	public void set(AbstractHorse horse, Personality personality) {
	}

	@Override
	public Personality roll(RandomSource random) {
		return Personality.STEADY;
	}
}
