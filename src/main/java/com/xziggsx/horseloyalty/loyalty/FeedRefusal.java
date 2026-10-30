package com.xziggsx.horseloyalty.loyalty;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A full horse refusing food within the feeder's cooldown: it snorts with a puff from its nose,
 * and the feeder sees above the hotbar how long until it's hungry for their food again.
 */
final class FeedRefusal {
	private static final int PUFFS = 3;

	private FeedRefusal() {
	}

	/** Plays the refusal for everyone nearby and tells the feeder the time left. */
	static void play(AbstractHorse horse, Player player) {
		if (!(horse.level() instanceof ServerLevel level)) {
			return;
		}
		level.playSound(null, horse, horse.isBaby() ? SoundEvents.HORSE_BREATHE_BABY : SoundEvents.HORSE_BREATHE,
				SoundSource.NEUTRAL, 1.0F, 0.8F + horse.getRandom().nextFloat() * 0.2F);
		// In front of the head, which sits a little past the front of the body.
		Vec3 ahead = Vec3.directionFromRotation(0.0F, horse.getYRot()).scale(horse.getBbWidth() * 0.9);
		level.sendParticles(ParticleTypes.POOF, horse.getX() + ahead.x, horse.getEyeY(), horse.getZ() + ahead.z,
				PUFFS, 0.05, 0.05, 0.05, 0.01);
		long seconds = (Loyalty.feedCooldownLeft(horse, player) + 19) / 20;
		player.sendOverlayMessage(Component.translatable("horseloyalty.loyalty.not_hungry", horse.getName(), seconds));
	}
}
