package com.xziggsx.horseloyalty.client.riding;

import com.xziggsx.horseloyalty.client.feedback.RidingToasts;
import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.personality.Personality;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Asking a horse for more speed, and the chance it refuses. Every "go faster" the rider asks for
 * goes through {@link #ask}, keyed by the gait it starts from: shifting up from a walk, a trot or a
 * canter, and pushing at a gallop (which counts as going up from a gallop). The config sets a fail
 * chance per gait and loyalty level; by default only walk to trot can fail, and only at loyalty 1 to 3.
 * <p>
 * A horse that refuses prances (rears up) and won't go faster for a while (the cooldown). A horse
 * whose personality is on the config's buck-off list may throw its rider off instead. One of these
 * lives on every horse (see {@code AbstractHorseRidingMixin}), which makes the horse act on the outcome.
 */
public final class GaitRaise {
	/** What the horse does when asked for more speed. */
	public enum Outcome {
		/** Goes faster as asked. */
		OK,
		/** Still unsettled from refusing before; nothing happens. */
		UNSETTLED,
		/** Refuses and rears up; the gait stays as it was. */
		PRANCE,
		/** Refuses and throws its rider off. */
		BUCK;

		/** Lang key part for the toast, such as "prance". */
		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * Game time until which the horse won't go faster, after it refused. Lives on the horse, not
	 * the ride, so getting off and on again doesn't skip it.
	 */
	private long blockedUntil;

	/**
	 * Asks the horse to go faster than {@code from}. While it's still unsettled from refusing before
	 * (a halt to walk is never held back by that) the rider gets a message; on a new refusal, a toast
	 * and the cooldown starts. The caller plays the sound and makes the horse rear or buck.
	 */
	public Outcome ask(AbstractHorse horse, Player player, Gait from) {
		long now = horse.level().getGameTime();
		if (from != Gait.HALT && now < blockedUntil) {
			player.sendOverlayMessage(Component.translatable("horseloyalty.gait.unsettled", horse.getName()));
			return Outcome.UNSETTLED;
		}
		Outcome outcome = roll(horse, player, from);
		if (outcome == Outcome.OK) {
			return outcome;
		}
		int cooldown = HorseLoyaltyConfig.get().gaitRaiseCooldownTicks();
		blockedUntil = now + cooldown;
		RidingToasts.show(player,
				Component.translatable("horseloyalty.toast." + outcome.id() + ".title", horse.getName()),
				Component.translatable("horseloyalty.toast." + outcome.id(), Math.round(cooldown / 20.0F)));
		return outcome;
	}

	/** Ticks until the horse will go faster again, 0 if it will now. */
	public int cooldownLeft(AbstractHorse horse) {
		return (int) Math.max(0, blockedUntil - horse.level().getGameTime());
	}

	/** Chance (0.06 = 6%) that the horse refuses this rider going faster from {@code from}. 0 when turned off. */
	public static double failChance(AbstractHorse horse, Player player, Gait from) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		return config.gaitRaiseEnabled() ? config.gaitRaiseFailChance(from.ordinal(), Loyalty.level(horse, player)) : 0;
	}

	/** Rolls whether the horse goes along, prances or bucks. */
	private static Outcome roll(AbstractHorse horse, Player player, Gait from) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		RandomSource random = horse.getRandom();
		if (random.nextDouble() >= failChance(horse, player, from)) {
			return Outcome.OK;
		}
		return config.bucksOff(Personality.of(horse)) && random.nextDouble() < config.buckOffChance()
				? Outcome.BUCK : Outcome.PRANCE;
	}
}
