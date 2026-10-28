package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.Block;

/**
 * Tamed horses need to eat. Feeding them, or leaving them (unridden) on a grazing block,
 * keeps them fed. Once they go too long without either, dark particles drift around
 * them and they slowly lose loyalty with every rider, down to level 1 at worst.
 * <p>
 * Grazing blocks are the block tag {@code horseloyalty:grazing_blocks}, so datapacks can
 * change them. Timings are in the user config, see {@link HorseLoyaltyConfig}.
 */
public final class Hunger {
	public static final TagKey<Block> GRAZING_BLOCKS = TagKey.create(Registries.BLOCK,
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "grazing_blocks"));

	/**
	 * @param lastFedTick  game time the horse last ate or grazed
	 * @param lastLossTick game time it last lost loyalty to hunger, 0 if it hasn't since eating
	 */
	public record State(long lastFedTick, long lastLossTick) {
		public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.LONG.fieldOf("last_fed_tick").forGetter(State::lastFedTick),
				Codec.LONG.optionalFieldOf("last_loss_tick", 0L).forGetter(State::lastLossTick)
		).apply(instance, State::new));
	}

	/** Server only, saved with the horse. Absent until the horse is first checked. */
	public static final AttachmentType<State> STATE = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "hunger"),
			builder -> builder.persistent(State.CODEC)
	);

	/** Work is spread out: hunger is checked once a second, not every tick. */
	private static final int CHECK_INTERVAL_TICKS = 20;

	private Hunger() {
	}

	/** Forces the class to load so the attachment is registered during mod init. */
	public static void init() {
	}

	/** Called every server tick for every horse. */
	public static void tick(AbstractHorse horse, ServerLevel level) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		if (!config.hungerEnabled() || !horse.isTamed() || !HorseScope.inScope(horse)
				|| horse.tickCount % CHECK_INTERVAL_TICKS != 0) {
			return;
		}
		long now = level.getGameTime();
		State state = horse.getAttached(STATE);
		if (state == null) {
			// First check since this horse was tamed (or since the mod was added): start fed.
			horse.setAttached(STATE, new State(now, 0L));
			return;
		}
		if (!horse.isVehicle() && horse.getBlockStateOn().is(GRAZING_BLOCKS)) {
			feed(horse);
			return;
		}

		long hungryAt = state.lastFedTick() + config.ticksUntilHungry();
		if (now < hungryAt) {
			return;
		}
		// Spread over the whole body from the back down. Ash drifts downwards, so
		// spawning it low hides it in the ground.
		level.sendParticles(ParticleTypes.ASH, horse.getX(), horse.getY() + horse.getBbHeight() * 0.6, horse.getZ(),
				16, horse.getBbWidth() * 0.5, horse.getBbHeight() * 0.35, horse.getBbWidth() * 0.5, 0.0);

		long lossFrom = Math.max(state.lastLossTick(), hungryAt);
		if (now - lossFrom >= config.loyaltyLossIntervalTicks()) {
			Loyalty.loseLoyalty(horse, config.loyaltyPointsLost());
			horse.setAttached(STATE, new State(state.lastFedTick(), now));
		}
	}

	/** The horse ate or grazed: it's fed again and the loss timer resets. */
	public static void feed(AbstractHorse horse) {
		horse.setAttached(STATE, new State(horse.level().getGameTime(), 0L));
	}

	/** True once the horse has gone longer than the configured time without eating or grazing. */
	public static boolean isHungry(AbstractHorse horse) {
		return ticksSinceFed(horse) >= HorseLoyaltyConfig.get().ticksUntilHungry();
	}

	/** Ticks since the horse last ate or grazed, 0 if not tracked yet. */
	public static long ticksSinceFed(AbstractHorse horse) {
		State state = horse.getAttached(STATE);
		return state == null ? 0 : horse.level().getGameTime() - state.lastFedTick();
	}

	/** Ticks until the next loyalty loss, only meaningful while hungry. */
	public static long ticksToNextLoss(AbstractHorse horse) {
		State state = horse.getAttached(STATE);
		if (state == null) {
			return 0;
		}
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		long lossFrom = Math.max(state.lastLossTick(), state.lastFedTick() + config.ticksUntilHungry());
		return Math.max(0, lossFrom + config.loyaltyLossIntervalTicks() - horse.level().getGameTime());
	}
}
