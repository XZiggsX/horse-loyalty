package com.xziggsx.horseloyalty.personality.api;

import com.xziggsx.horseloyalty.core.link.LinkKey;
import com.xziggsx.horseloyalty.core.link.Links;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * A horse's personality, as other features see it. Provided by personality; read by genetics
 * (the temperament genes), the inventory label, horse info and the dev tools. While personality is
 * off every horse acts steady, nothing is revealed and nothing is stored (see {@link NeutralPersonalityView}).
 */
public interface PersonalityView {
	LinkKey<PersonalityView> KEY = new LinkKey<>("personality:view", PersonalityView.class, new NeutralPersonalityView());

	/** The view as it is now: the personality feature's, or the neutral one while it's off. */
	static PersonalityView get() {
		return Links.get(KEY);
	}

	/** The horse's personality as far as behaviour goes: STEADY for a horse without one. Works on both sides. */
	Personality of(AbstractHorse horse);

	/** The personality if this player knows the horse well enough to see it, else empty. Works on both sides. */
	Optional<Personality> revealedTo(AbstractHorse horse, Player player);

	/** The personality saved on the horse, or null if it has none yet. */
	Personality stored(AbstractHorse horse);

	/** Saves a personality on the horse, as genetics does when its genes decide it. Server side. */
	void set(AbstractHorse horse, Personality personality);

	/** A random personality, weighted by the config. */
	Personality roll(RandomSource random);
}
