package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Loyalty levels 0 to 5, tracked separately for every player who rides or feeds the horse.
 * <ul>
 *   <li>0: stray (not tamed)</li>
 *   <li>1: tamed, the starting level for every player</li>
 *   <li>2 to 5: earned by feeding and riding</li>
 * </ul>
 * How fast loyalty builds is set in the user config, see {@link LoyaltySettings}.
 * Level names live in the lang file under {@code horseloyalty.loyalty.level.N}.
 */
public final class Loyalty {
	public static final int MAX_LEVEL = 5;

	/** Minimum distance per tick (squared) for a ride tick to count, so standing still doesn't. */
	public static final double MIN_RIDE_MOVE_SQR = 0.05 * 0.05;
	/**
	 * Most riders a horse remembers. When a new rider would go over this, the one with
	 * the fewest points is forgotten. Keeps a much-borrowed horse from growing forever.
	 */
	public static final int MAX_REMEMBERED_RIDERS = 16;

	/** Full per-player stats. Server only, saved with the horse. */
	public static final AttachmentType<Map<UUID, LoyaltyData>> DATA = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "riders"),
			builder -> builder.persistent(Codec.unboundedMap(UUIDUtil.STRING_CODEC, LoyaltyData.CODEC))
					.initializer(Map::of)
	);

	/**
	 * Earned level (1 to 5) per player, ignoring whether the horse is still tamed.
	 * Kept separate from {@link #DATA} so only these small numbers are synced to clients,
	 * and only when one changes, instead of every ride tick.
	 */
	public static final AttachmentType<Map<UUID, Integer>> EARNED_LEVELS = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "earned_levels"),
			builder -> builder.persistent(Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT))
					.initializer(Map::of)
					.syncWith(ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.VAR_INT),
							AttachmentSyncPredicate.all())
	);

	private Loyalty() {
	}

	/** Forces the class to load so the attachments are registered during mod init. */
	public static void init() {
	}

	/** This player's stats with the horse (empty if they have none). Server side only. */
	public static LoyaltyData get(AbstractHorse horse, Player player) {
		// OrElse, not OrCreate: just looking must not save an empty record on every horse.
		return horse.getAttachedOrElse(DATA, Map.of()).getOrDefault(player.getUUID(), LoyaltyData.EMPTY);
	}

	/** This player's level with the horse. Works on both sides, using the synced levels. */
	public static int level(AbstractHorse horse, Player player) {
		if (!horse.isTamed()) {
			return 0;
		}
		Map<UUID, Integer> levels = horse.getAttachedOrElse(EARNED_LEVELS, Map.of());
		return levels.getOrDefault(player.getUUID(), 1);
	}

	/** The level (1 to 5) these stats are worth, ignoring whether the horse is tamed. */
	public static int earnedLevel(LoyaltyData data) {
		int points = data.points();
		LoyaltySettings config = LoyaltySettings.get();
		int level = 1;
		for (int next = 2; next <= MAX_LEVEL; next++) {
			if (points >= config.pointsNeeded(next)) {
				level = next;
			}
		}
		return level;
	}

	/** Translated level name, such as "Trusting", from the lang key horseloyalty.loyalty.level.N. */
	public static Component levelName(int level) {
		return Component.translatable("horseloyalty.loyalty.level." + level);
	}

	/** Whether this player's feed cooldown has passed, so the next feed counts for loyalty. */
	public static boolean canCountFeed(AbstractHorse horse, Player player) {
		long last = get(horse, player).lastFedTick();
		return last == 0L || horse.level().getGameTime() - last >= LoyaltySettings.get().feedCooldownTicks();
	}

	/** Ticks until this player's next feed counts for loyalty, 0 once the cooldown has passed. */
	public static long feedCooldownLeft(AbstractHorse horse, Player player) {
		long last = get(horse, player).lastFedTick();
		if (last == 0L) {
			return 0L;
		}
		return Math.max(0L, last + LoyaltySettings.get().feedCooldownTicks() - horse.level().getGameTime());
	}

	/** Adds one counted feed for this player, worth the usual points per feed. Server side. */
	public static void recordFeed(AbstractHorse horse, Player player) {
		recordFeed(horse, player, LoyaltySettings.get().pointsPerFeed());
	}

	/**
	 * Adds one counted feed for this player, worth {@code points} (see {@code FeedModifiers}). More
	 * than the usual points per feed are kept as bonus points; fewer, or a negative number, as a
	 * loss, which never takes the player below level 1. Server side.
	 */
	public static void recordFeed(AbstractHorse horse, Player player, int points) {
		int before = level(horse, player);
		LoyaltyData next = get(horse, player).withFeed(horse.level().getGameTime());
		int extra = points - LoyaltySettings.get().pointsPerFeed();
		if (extra > 0) {
			next = next.withBonus(extra);
		} else if (extra < 0) {
			next = next.withLoss(-extra);
		}
		update(horse, player, next, true);
		int after = level(horse, player);
		if (after < before && player instanceof ServerPlayer serverPlayer) {
			notifyChange(horse, serverPlayer, after, false);
		}
	}

	/** Adds one tick of riding for this player. Server side, called every tick while they ride. */
	public static void recordRideTick(AbstractHorse horse, Player player) {
		update(horse, player, get(horse, player).withRideTick(), true);
	}

	/**
	 * Gives a player bonus points with a horse without the level-up celebration, such as
	 * a newly born foal's head start (see {@code Family} in foals). Server side.
	 */
	public static void startWith(AbstractHorse horse, Player player, int bonusPoints) {
		update(horse, player, get(horse, player).withBonus(bonusPoints), false);
	}

	/**
	 * Sets a player's points with a horse, for the commands: changes their bonus points (and, below
	 * what they earned, their losses) so the total comes out at {@code points}. A level change shows
	 * the usual level-up or level-down toast. Server side.
	 */
	public static void setPoints(AbstractHorse horse, ServerPlayer player, int points) {
		int before = level(horse, player);
		update(horse, player, get(horse, player).withPoints(Math.max(0, points)), false);
		int after = level(horse, player);
		if (after != before) {
			notifyChange(horse, player, after, after > before);
		}
	}

	/** The horse forgets a rider entirely: their points, feeds and level. Server side. */
	public static void forget(AbstractHorse horse, UUID player) {
		Map<UUID, LoyaltyData> riders = new HashMap<>(horse.getAttachedOrElse(DATA, Map.of()));
		riders.remove(player);
		horse.setAttached(DATA, riders);
		Map<UUID, Integer> levels = new HashMap<>(horse.getAttachedOrElse(EARNED_LEVELS, Map.of()));
		levels.remove(player);
		horse.setAttached(EARNED_LEVELS, levels);
	}

	/** Saves a player's new stats, refreshes the synced levels and, if {@code celebrate}, celebrates a level up. */
	private static void update(AbstractHorse horse, Player player, LoyaltyData next, boolean celebrate) {
		UUID id = player.getUUID();
		Map<UUID, LoyaltyData> riders = new HashMap<>(horse.getAttachedOrElse(DATA, Map.of()));
		riders.put(id, next);
		forgetExtraRiders(riders, id);
		horse.setAttached(DATA, riders);

		Map<UUID, Integer> levels = horse.getAttachedOrElse(EARNED_LEVELS, Map.of());
		int before = levels.getOrDefault(id, 1);
		int after = earnedLevel(next);
		if (after != before || !levels.keySet().equals(riders.keySet())) {
			Map<UUID, Integer> updated = new HashMap<>();
			riders.forEach((uuid, data) -> updated.put(uuid, earnedLevel(data)));
			horse.setAttached(EARNED_LEVELS, updated);
		}
		if (celebrate && after > before) {
			onLevelUp(horse, player, after);
		}
	}

	/**
	 * Takes points from every rider at once (hunger). A rider who drops a level while
	 * riding this horse sees it above the hotbar. Levels never go below 1, and the horse stays tamed.
	 */
	public static void loseLoyalty(AbstractHorse horse, int points) {
		Map<UUID, LoyaltyData> current = horse.getAttachedOrElse(DATA, Map.of());
		if (current.isEmpty() || points <= 0) {
			return;
		}
		Map<UUID, LoyaltyData> riders = new HashMap<>(current);
		riders.replaceAll((id, data) -> data.withLoss(points));
		horse.setAttached(DATA, riders);

		Map<UUID, Integer> before = horse.getAttachedOrElse(EARNED_LEVELS, Map.of());
		Map<UUID, Integer> after = new HashMap<>();
		riders.forEach((id, data) -> after.put(id, earnedLevel(data)));
		if (after.equals(before)) {
			return;
		}
		horse.setAttached(EARNED_LEVELS, after);
		if (horse.level() instanceof ServerLevel serverLevel) {
			boolean anyDropped = false;
			for (Map.Entry<UUID, Integer> entry : after.entrySet()) {
				int level = entry.getValue();
				if (level >= before.getOrDefault(entry.getKey(), 1)) {
					continue;
				}
				anyDropped = true;
				// Only for the rider currently on this horse, so nobody gets messages about a horse they aren't with.
				ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
				if (player != null && player.getVehicle() == horse) {
					notifyChange(horse, player, level, false);
				}
			}
			if (anyDropped) {
				// A low, unhappy snort for everyone nearby.
				serverLevel.playSound(null, horse, SoundEvents.HORSE_BREATHE, horse.getSoundSource(), 0.8F, 0.6F);
			}
		}
	}

	/** Drops the lowest scoring riders (never {@code keep}) until at most MAX_REMEMBERED_RIDERS remain. */
	private static void forgetExtraRiders(Map<UUID, LoyaltyData> riders, UUID keep) {
		while (riders.size() > MAX_REMEMBERED_RIDERS) {
			riders.entrySet().stream()
					.filter(entry -> !entry.getKey().equals(keep))
					.min(Comparator.comparingInt(entry -> entry.getValue().points()))
					.ifPresent(entry -> riders.remove(entry.getKey()));
		}
	}

	/** Heart particles and a happy whinny for everyone nearby, and a toast for the player. */
	private static void onLevelUp(AbstractHorse horse, Player player, int level) {
		if (horse.level() instanceof ServerLevel serverLevel) {
			// Entity event 7 is the vanilla "tamed" heart burst.
			serverLevel.broadcastEntityEvent(horse, (byte) 7);
			// The horse's own voice, so donkeys bray and mules sound like mules.
			horse.playAmbientSound();
		}
		if (player instanceof ServerPlayer serverPlayer) {
			notifyChange(horse, serverPlayer, level, true);
		}
	}

	/**
	 * Tells a player their level with a horse changed: a toast on their client, or a message above
	 * the hotbar if their client can't show it (it should always have the mod, so this is a fallback).
	 */
	private static void notifyChange(AbstractHorse horse, ServerPlayer player, int level, boolean up) {
		if (ServerPlayNetworking.canSend(player, LoyaltyChangePayload.TYPE)) {
			ServerPlayNetworking.send(player, new LoyaltyChangePayload(horse.getId(), level, up));
		} else {
			player.sendOverlayMessage(Component.translatable(up ? "horseloyalty.loyalty.level_up" : "horseloyalty.loyalty.level_down",
					horse.getName(), level, MAX_LEVEL, levelName(level)));
		}
	}

	/** Short status line for this player, shown when they mount. */
	public static Component describe(AbstractHorse horse, Player player) {
		LoyaltyData data = get(horse, player);
		int level = level(horse, player);
		return Component.translatable("horseloyalty.loyalty.status",
				level, MAX_LEVEL, levelName(level), data.feeds(), data.rideMinutes());
	}
}
