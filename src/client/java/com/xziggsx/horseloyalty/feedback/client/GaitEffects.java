package com.xziggsx.horseloyalty.feedback.client;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.RideView;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Particles for ridden horses, on every client: dust kicked up behind the hooves at a canter or
 * gallop, splashes in shallow water from a trot, and little breath puffs in snowy places from a
 * trot. Uses the synced gait (see {@link RideView}), so everyone sees them, not just the rider.
 * Vanilla's particle setting (All, Decreased, Minimal) still applies.
 */
public final class GaitEffects {
	/** Only horses this close to you (in blocks) get particles. */
	private static final double MAX_DISTANCE = 48;
	/** Below this speed (blocks per tick, squared) a horse counts as standing still. */
	private static final double MIN_MOVE_SQR = 0.04 * 0.04;

	private GaitEffects() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(GaitEffects::tick);
	}

	private static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		if (level == null || minecraft.player == null || minecraft.isPaused()
				|| !(config.gaitParticles() || config.coldBreath())) {
			return;
		}
		for (Entity entity : level.entitiesForRendering()) {
			if (entity instanceof AbstractHorse horse && horse.isVehicle() && HorseScope.inScope(horse)
					&& horse.distanceToSqr(minecraft.player) < MAX_DISTANCE * MAX_DISTANCE) {
				tickHorse(level, horse, config);
			}
		}
	}

	private static void tickHorse(ClientLevel level, AbstractHorse horse, HorseLoyaltyConfig config) {
		Gait gait = RideView.get().gait(horse);
		double dx = horse.getX() - horse.xo;
		double dz = horse.getZ() - horse.zo;
		if (gait == null || !gait.atLeast(Gait.TROT) || dx * dx + dz * dz < MIN_MOVE_SQR) {
			return;
		}
		RandomSource random = horse.getRandom();
		if (config.gaitParticles()) {
			if (horse.isInWater()) {
				splash(level, horse, random);
			} else if (gait.atLeast(Gait.CANTER) && horse.onGround()) {
				dust(level, horse, gait, dx, dz, random);
			}
		}
		if (config.coldBreath()) {
			breath(level, horse, gait, dx, dz, random);
		}
	}

	/** Clumps of the block underfoot thrown up behind the hooves, like a sprinting player's. */
	private static void dust(ClientLevel level, AbstractHorse horse, Gait gait, double dx, double dz, RandomSource random) {
		BlockState ground = level.getBlockState(horse.getOnPos());
		if (ground.getRenderShape() == RenderShape.INVISIBLE) {
			return;
		}
		int count = gait == Gait.GALLOP ? 2 : 1;
		float width = horse.getBbWidth();
		for (int i = 0; i < count; i++) {
			double x = horse.getX() + (random.nextDouble() - 0.5) * width;
			double z = horse.getZ() + (random.nextDouble() - 0.5) * width;
			level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
					x, horse.getY() + 0.1, z, -dx * 4.0, 1.5, -dz * 4.0);
		}
	}

	/** Splashes around the legs while moving through water. */
	private static void splash(ClientLevel level, AbstractHorse horse, RandomSource random) {
		float width = horse.getBbWidth();
		for (int i = 0; i < 3; i++) {
			double x = horse.getX() + (random.nextDouble() - 0.5) * width * 1.4;
			double z = horse.getZ() + (random.nextDouble() - 0.5) * width * 1.4;
			level.addParticle(ParticleTypes.SPLASH, x, horse.getY() + 0.4, z, 0.0, 0.2, 0.0);
		}
	}

	/** A few small puffs from the nose now and then, faster gaits breathing harder. Only where it snows. */
	private static void breath(ClientLevel level, AbstractHorse horse, Gait gait, double dx, double dz, RandomSource random) {
		int interval = switch (gait) {
			case GALLOP -> 20;
			case CANTER -> 30;
			default -> 40;
		};
		// Offset by entity id so a group of horses doesn't breathe in step.
		if ((horse.tickCount + horse.getId()) % interval != 0) {
			return;
		}
		// The nose is ahead of the body, about at the top of the hitbox while moving.
		float yaw = horse.getYRot() * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(yaw);
		double forwardZ = Mth.cos(yaw);
		double reach = horse.getBbWidth() * 0.5 + 0.6;
		double x = horse.getX() + forwardX * reach;
		double y = horse.getY() + horse.getBbHeight() * 0.9;
		double z = horse.getZ() + forwardZ * reach;
		BlockPos pos = BlockPos.containing(x, y, z);
		if (!level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel())) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			level.addParticle(ParticleTypes.WHITE_SMOKE,
					x + (random.nextDouble() - 0.5) * 0.15, y + (random.nextDouble() - 0.5) * 0.15, z + (random.nextDouble() - 0.5) * 0.15,
					dx + forwardX * 0.04, 0.01, dz + forwardZ * 0.04);
		}
	}
}
