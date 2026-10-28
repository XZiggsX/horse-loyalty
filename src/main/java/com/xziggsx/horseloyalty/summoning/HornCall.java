package com.xziggsx.horseloyalty.summoning;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.loyalty.api.Bond;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Blowing a bound horn (see {@link HornBinding}) calls the horse. Vanilla still plays the horn
 * sound and starts its cooldown; this runs after that, on the server.
 * <p>
 * Only a horse that is loaded answers. A nearby horse just runs over. One further away is moved to
 * open ground behind the player, out of sight, and runs to them from there (see
 * {@link ComeWhenCalledGoal}). Nothing is moved in the Nether or while the player is underground,
 * and a horse is only ever placed where it fits, on solid ground, out of water.
 */
public final class HornCall {
	/** Blocks behind the player to try placing the horse, furthest first. */
	private static final int[] SPOT_DISTANCES = {16, 13, 10, 7};
	/** Degrees off straight behind the player to try, at each distance. All are well outside the view. */
	private static final int[] SPOT_ANGLES = {0, 20, -20, 40, -40, 60, -60, 80, -80};
	/** Most blocks a spot may be above or below the player, so the horse can reach them. */
	private static final int MAX_HEIGHT_DIFFERENCE = 4;
	/** More blocks than this below the surface counts as underground, even above sea level. */
	private static final int UNDERGROUND_DEPTH = 6;

	/** Who called the horse and until when (game time). Server only, not saved. */
	public record Call(UUID caller, long until) {
	}

	public static final AttachmentType<Call> CALL = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "horn_call")
	);

	private HornCall() {
	}

	/** Forces the class to load so the attachment is registered during mod init. */
	public static void init() {
	}

	/** Calls the horse with this UUID to the player. The player's horn has already sounded. */
	public static void call(ServerPlayer player, UUID horseId) {
		ServerLevel level = player.level();
		AbstractHorse horse = find(level.getServer(), horseId);
		if (horse == null) {
			say(player, "horseloyalty.horn.no_answer");
			return;
		}
		Component name = horse.getName();
		SummoningSettings settings = SummoningSettings.get();
		int loyalty = Bond.get().level(horse, player);
		if (loyalty < settings.minLevel()) {
			say(player, "horseloyalty.horn.not_trusted", name, settings.minLevel(), Bond.MAX_LEVEL, Bond.levelName(settings.minLevel()));
			return;
		}
		if (horse.isVehicle()) {
			say(player, "horseloyalty.horn.ridden", name);
			return;
		}
		if (horse.isLeashed() || horse.isPassenger()) {
			say(player, "horseloyalty.horn.tied", name);
			return;
		}
		boolean sameLevel = horse.level() == level;
		if (sameLevel && horse.distanceToSqr(player) <= settings.runOverDistance() * settings.runOverDistance()) {
			comeTo(horse, player);
			say(player, "horseloyalty.horn.coming", name);
			return;
		}
		if (level.dimension() == Level.NETHER) {
			say(player, "horseloyalty.horn.nether", name);
			return;
		}
		if (isUnderground(level, player)) {
			say(player, "horseloyalty.horn.underground", name);
			return;
		}
		if (!sameLevel && loyalty < settings.crossDimensionLevel()) {
			say(player, "horseloyalty.horn.other_dimension", name);
			return;
		}
		Vec3 spot = findSpot(level, horse, player);
		if (spot == null) {
			say(player, "horseloyalty.horn.no_room", name);
			return;
		}
		float facePlayer = (float) (Mth.atan2(player.getZ() - spot.z, player.getX() - spot.x) * Mth.RAD_TO_DEG) - 90.0F;
		// Same entity within a dimension; across dimensions vanilla swaps in a copy with the same UUID and data.
		Entity moved = horse.teleport(new TeleportTransition(level, spot, Vec3.ZERO, facePlayer, 0.0F, TeleportTransition.DO_NOTHING));
		if (!(moved instanceof AbstractHorse arrived)) {
			say(player, "horseloyalty.horn.no_room", name);
			return;
		}
		arrived.resetFallDistance();
		arrived.setYHeadRot(facePlayer);
		arrived.setYBodyRot(facePlayer);
		comeTo(arrived, player);
		say(player, "horseloyalty.horn.coming", name);
	}

	/** The horse with this UUID if it's loaded and alive in any dimension, else null. */
	private static AbstractHorse find(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(id) instanceof AbstractHorse horse && horse.isAlive() && HorseScope.inScope(horse)) {
				return horse;
			}
		}
		return null;
	}

	/** Below sea level, or well below the open surface (a cave in a hillside). */
	private static boolean isUnderground(ServerLevel level, ServerPlayer player) {
		int y = player.getBlockY();
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.getBlockX(), player.getBlockZ());
		return y < level.getSeaLevel() || y < surface - UNDERGROUND_DEPTH;
	}

	/**
	 * Open ground behind the player where the horse fits: on the surface (never in a cave), within a
	 * few blocks of the player's height, not in or next to a liquid, and on a block it can stand on.
	 * Null if there is none.
	 */
	private static Vec3 findSpot(ServerLevel level, AbstractHorse horse, ServerPlayer player) {
		for (int distance : SPOT_DISTANCES) {
			for (int angle : SPOT_ANGLES) {
				// Minecraft yaw: 0 faces +Z, 90 faces -X. Adding 180 points behind the player.
				float yaw = (player.getYRot() + 180.0F + angle) * Mth.DEG_TO_RAD;
				int x = Mth.floor(player.getX() - Mth.sin(yaw) * distance);
				int z = Mth.floor(player.getZ() + Mth.cos(yaw) * distance);
				// Never look at an unloaded chunk: reading its height would load it.
				if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
					continue;
				}
				BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
				if (Math.abs(feet.getY() - player.getBlockY()) > MAX_HEIGHT_DIFFERENCE) {
					continue;
				}
				Vec3 spot = Vec3.atBottomCenterOf(feet);
				if (fits(level, horse, feet, spot)) {
					return spot;
				}
			}
		}
		return null;
	}

	/** Whether the horse's whole body fits at {@code spot}, dry, standing on solid, safe ground. */
	private static boolean fits(ServerLevel level, AbstractHorse horse, BlockPos feet, Vec3 spot) {
		AABB body = horse.getDimensions(horse.getPose()).makeBoundingBox(spot);
		return level.noCollision(horse, body)
				&& !level.containsAnyLiquid(body.inflate(1.0, 0.0, 1.0))
				&& WalkNodeEvaluator.getPathTypeStatic(horse, feet) == PathType.WALKABLE;
	}

	/** Sets the horse running to the player, see {@link ComeWhenCalledGoal}. */
	private static void comeTo(AbstractHorse horse, ServerPlayer player) {
		horse.setEating(false);
		horse.setAttached(CALL, new Call(player.getUUID(), horse.level().getGameTime() + SummoningSettings.get().callTicks()));
	}

	private static void say(ServerPlayer player, String key, Object... args) {
		player.sendOverlayMessage(Component.translatable(key, args));
	}
}
