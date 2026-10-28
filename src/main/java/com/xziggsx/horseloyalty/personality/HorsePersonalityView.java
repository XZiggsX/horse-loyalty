package com.xziggsx.horseloyalty.personality;

import com.xziggsx.horseloyalty.personality.api.Personality;
import com.xziggsx.horseloyalty.personality.api.PersonalityView;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** The personality feature's {@link PersonalityView}, over {@link Personalities}. */
final class HorsePersonalityView implements PersonalityView {
	@Override
	public Personality of(AbstractHorse horse) {
		return Personalities.of(horse);
	}

	@Override
	public Optional<Personality> revealedTo(AbstractHorse horse, Player player) {
		return Personalities.isRevealedTo(horse, player) ? Optional.of(Personalities.of(horse)) : Optional.empty();
	}

	@Override
	public Personality stored(AbstractHorse horse) {
		return horse.getAttached(Personalities.DATA);
	}

	@Override
	public void set(AbstractHorse horse, Personality personality) {
		horse.setAttached(Personalities.DATA, personality);
	}

	@Override
	public Personality roll(RandomSource random) {
		return Personalities.roll(random);
	}
}
