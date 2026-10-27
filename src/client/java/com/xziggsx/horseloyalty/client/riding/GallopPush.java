package com.xziggsx.horseloyalty.client.riding;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Pushing a galloping horse with W taps. Each tap adds a step of extra speed, up to the configured
 * most. Every tap, also at the most, restarts the hold timer (see {@link Riding#gallopPushTicks});
 * once the rider stops tapping that long the extra speed fades away exponentially, and a tap while
 * it fades builds on what's left. Leaving the gallop ends the push at once.
 * <p>
 * While pushed the horse breathes hard, for the rider only. The HUD gait bar glows with the push
 * (see {@code RidingHud}); with the HUD off, a message above the hotbar says it instead.
 * One of these lives on every horse (see {@code AbstractHorseRidingMixin}).
 */
public final class GallopPush {
	/** Most ticks between breaths while pushed; up to 6 fewer at the full push. */
	private static final int SOUND_INTERVAL = 16;
	/** A tap won't add a breath sooner than this many ticks after the last. */
	private static final int SOUND_MIN_GAP = 6;
	/** Below half a percent nobody can tell; that's a normal gallop. */
	private static final double MIN_PUSH = 0.005;

	/** Extra share of gallop speed (0.02 = 102%). 0 when not pushing. */
	private double push;
	/** Ticks until the push starts to fade unless the rider taps W again. */
	private int holdTicks;
	/** Ticks since the last breath, so taps and the steady rhythm don't pile up. */
	private int soundTicks;

	/** Extra share of gallop speed (0.02 = 102%), 0 when not pushing. */
	public double extra() {
		return push;
	}

	/** How hard the horse is pushed, from 0 (not at all) to 1 (the most the config allows). */
	public double effort() {
		double max = HorseLoyaltyConfig.get().gallopPushMax();
		return max > 0 ? Math.min(1, push / max) : 0;
	}

	/** Ends any push, for a new ride. */
	public void reset() {
		push = 0;
		holdTicks = 0;
	}

	/**
	 * A W tap at a gallop. {@code askForMore} is asked before each step up and says whether the
	 * horse goes along (see {@link GaitRaise}). Returns whether the horse took the tap, so the
	 * caller can count it as the rider paying attention.
	 */
	public boolean tap(AbstractHorse horse, Player player, BooleanSupplier askForMore) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		if (!config.gallopPushEnabled() || config.gallopPushStep() <= 0 || config.gallopPushMax() <= 0) {
			return false;
		}
		boolean starting = push <= 0;
		if (push < config.gallopPushMax()) {
			if (!askForMore.getAsBoolean()) {
				return false;
			}
			push = Math.min(config.gallopPushMax(), push + config.gallopPushStep());
		}
		holdTicks = Riding.gallopPushTicks(horse, player);
		// A tap answers with a breath straight away, unless one just played.
		if (starting || soundTicks >= SOUND_MIN_GAP) {
			playSound(horse);
		}
		if (!config.ridingHud()) {
			player.sendOverlayMessage(Component.translatable("horseloyalty.gait.push",
					Gait.GALLOP.displayName(), Math.round((1 + push) * 100)));
		}
		return true;
	}

	/** Every riding tick: holds, fades or ends the push, and keeps the horse breathing hard. */
	public void tick(AbstractHorse horse, Player player, boolean galloping) {
		if (push <= 0) {
			return;
		}
		if (!galloping) {
			reset();
			return;
		}
		if (holdTicks > 0) {
			holdTicks--;
		} else {
			// Shrinks by the same share every tick: quick at first, then easing into a normal gallop.
			push *= HorseLoyaltyConfig.get().gallopPushFadePerTick();
			if (push < MIN_PUSH) {
				reset();
				if (!HorseLoyaltyConfig.get().ridingHud()) {
					player.sendOverlayMessage(Component.translatable("horseloyalty.gait.push_end", Gait.GALLOP.displayName()));
				}
				return;
			}
		}
		// Breathes faster the harder it's pushed.
		if (++soundTicks >= SOUND_INTERVAL - Math.round(effort() * 6)) {
			playSound(horse);
		}
	}

	/**
	 * A heavy, straining breath: mostly the vanilla gallop breath, now and then the idle snort for
	 * variety, each with a little random pitch so it doesn't sound like a loop. Quieter than a real
	 * gallop, and higher and louder the harder the horse is pushed. Only the rider hears it.
	 */
	private void playSound(AbstractHorse horse) {
		soundTicks = 0;
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		float volume = config.gallopPushSoundVolume();
		if (!config.gaitSounds() || volume <= 0) {
			return;
		}
		float effort = (float) effort();
		RandomSource random = horse.getRandom();
		// The idle sound is a louder snort, so it plays a bit quieter than the breath.
		boolean snort = random.nextInt(3) == 0;
		SoundEvent sound = snort ? SoundEvents.HORSE_AMBIENT : SoundEvents.HORSE_BREATHE;
		float pitch = horse.getVoicePitch() * (1.0F + 0.15F * effort) * (0.94F + random.nextFloat() * 0.12F);
		horse.level().playLocalSound(horse, sound, horse.getSoundSource(),
				volume * (0.6F + 0.4F * effort) * (snort ? 0.7F : 1.0F), pitch);
	}
}
