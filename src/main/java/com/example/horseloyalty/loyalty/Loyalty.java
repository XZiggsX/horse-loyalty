package com.example.horseloyalty.loyalty;

import com.example.horseloyalty.HorseLoyaltyMod;
import com.example.horseloyalty.config.HorseLoyaltyConfig;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Player;

/**
 * Loyalty levels 0 to 5, tracked separately for every player who rides or feeds the horse.
 * <ul>
 *   <li>0: stray (not tamed)</li>
 *   <li>1: tamed, the starting level for every player</li>
 *   <li>2 to 5: earned by feeding and riding</li>
 * </ul>
 * How fast loyalty builds is set in the user config, see {@link HorseLoyaltyConfig}.
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

	/** Horses, donkeys, mules and the undead horses. Llamas and camels are left alone. */
	public static boolean appliesTo(AbstractHorse horse) {
		return !(horse instanceof Llama) && !(horse instanceof Camel);
	}

	/** This player's stats with the horse (empty if they have none). Server side only. */
	public static LoyaltyData get(AbstractHorse horse, Player player) {
		return horse.getAttachedOrCreate(DATA).getOrDefault(player.getUUID(), LoyaltyData.EMPTY);
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
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
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
		return last == 0L || horse.level().getGameTime() - last >= HorseLoyaltyConfig.get().feedCooldownTicks();
	}

	/** Adds one counted feed for this player. Server side. */
	public static void recordFeed(AbstractHorse horse, Player player) {
		update(horse, player, get(horse, player).withFeed(horse.level().getGameTime()));
	}

	/** Adds one tick of riding for this player. Server side, called every tick while they ride. */
	public static void recordRideTick(AbstractHorse horse, Player player) {
		update(horse, player, get(horse, player).withRideTick());
	}

	/** Saves a player's new stats, refreshes the synced levels and celebrates a level up. */
	private static void update(AbstractHorse horse, Player player, LoyaltyData next) {
		UUID id = player.getUUID();
		Map<UUID, LoyaltyData> riders = new HashMap<>(horse.getAttachedOrCreate(DATA));
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
		if (after > before) {
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
			after.forEach((id, level) -> {
				// Above the hotbar, not in chat, and only for the rider currently on this horse,
				// so nobody gets messages about a horse they aren't with.
				ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(id);
				if (player != null && player.getVehicle() == horse && level < before.getOrDefault(id, 1)) {
					player.sendOverlayMessage(Component.translatable("horseloyalty.loyalty.level_down",
							horse.getName(), level, MAX_LEVEL, levelName(level)));
				}
			});
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

	/** Heart particles for everyone nearby, and a message above the player's hotbar. */
	private static void onLevelUp(AbstractHorse horse, Player player, int level) {
		if (horse.level() instanceof ServerLevel serverLevel) {
			// Entity event 7 is the vanilla "tamed" heart burst.
			serverLevel.broadcastEntityEvent(horse, (byte) 7);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			// Above the hotbar rather than in chat.
			serverPlayer.sendOverlayMessage(Component.translatable("horseloyalty.loyalty.level_up",
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
