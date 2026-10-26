package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Who a bred foal belongs to. Written once at birth and kept for life, so later features
 * (personality inheritance, lineage) can use it.
 * <p>
 * Minecraft horses have no sexes, so instead of a fixed mother the foal picks one of its
 * two parents at random to follow. Players can roleplay that parent as the mother or the
 * father. Foals that spawned in the wild have no family and keep vanilla behaviour.
 *
 * @param parentA  the parent that ran the breed (the foal spawns at its position)
 * @param parentB  the other parent
 * @param followed the parent the foal follows, parentA or parentB
 * @param breeder  the player who bred it; the foal is born tamed to them
 */
public record Family(UUID parentA, UUID parentB, UUID followed, Optional<UUID> breeder) {
	public static final Codec<Family> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.STRING_CODEC.fieldOf("parent_a").forGetter(Family::parentA),
			UUIDUtil.STRING_CODEC.fieldOf("parent_b").forGetter(Family::parentB),
			UUIDUtil.STRING_CODEC.fieldOf("followed").forGetter(Family::followed),
			UUIDUtil.STRING_CODEC.optionalFieldOf("breeder").forGetter(Family::breeder)
	).apply(instance, Family::new));

	/** Server only, saved with the horse. Absent on horses that weren't bred with the mod. */
	public static final AttachmentType<Family> DATA = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "family"),
			builder -> builder.persistent(CODEC)
	);

	/** Forces the class to load so the attachment is registered during mod init. */
	public static void init() {
	}

	/** The parent the foal doesn't follow. */
	public UUID otherParent() {
		return followed.equals(parentA) ? parentB : parentA;
	}

	/**
	 * Sets up a newly bred foal: records its family, picks which parent it follows, and if the
	 * breeding player is online, tames it to them with a share of their loyalty with both parents.
	 * Called on the server before the foal is added to the world.
	 *
	 * @param parentA the parent that ran the breed
	 * @param breeder the player who bred them, or null if nobody online did
	 */
	public static void onBorn(AbstractHorse foal, AbstractHorse parentA, AbstractHorse parentB, ServerPlayer breeder) {
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		if (!config.foalsEnabled() || !Loyalty.appliesTo(foal)) {
			return;
		}
		UUID followed = foal.getRandom().nextBoolean() ? parentA.getUUID() : parentB.getUUID();
		foal.setAttached(DATA, new Family(parentA.getUUID(), parentB.getUUID(), followed,
				Optional.ofNullable(breeder).map(ServerPlayer::getUUID)));
		if (breeder == null) {
			return;
		}

		foal.setOwner(breeder);
		foal.setTamed(true);
		// A share of the average of what the breeder built up with each parent.
		int average = (Loyalty.get(parentA, breeder).points() + Loyalty.get(parentB, breeder).points()) / 2;
		int bonus = (int) Math.round(average * config.foalParentLoyaltyShare());
		Loyalty.startWith(foal, breeder, bonus);

		int level = Loyalty.level(foal, breeder);
		breeder.sendOverlayMessage(Component.translatable("horseloyalty.foal.born",
				level, Loyalty.MAX_LEVEL, Loyalty.levelName(level)));
	}

	/** The parent this foal follows, or the other one if that one is gone. Empty if neither is loaded. */
	public static Optional<AbstractHorse> parentToFollow(AbstractHorse foal, ServerLevel level) {
		Family family = foal.getAttached(DATA);
		if (family == null) {
			return Optional.empty();
		}
		return findHorse(level, family.followed()).or(() -> findHorse(level, family.otherParent()));
	}

	/** The player who bred this foal, if they're online in the same dimension. */
	public static Optional<ServerPlayer> breeder(AbstractHorse foal, ServerLevel level) {
		Family family = foal.getAttached(DATA);
		if (family == null) {
			return Optional.empty();
		}
		return family.breeder()
				.map(id -> level.getEntity(id) instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()
						? player : null);
	}

	private static Optional<AbstractHorse> findHorse(ServerLevel level, UUID id) {
		return level.getEntity(id) instanceof AbstractHorse horse && horse.isAlive()
				? Optional.of(horse) : Optional.empty();
	}

	/** A foal our goals look after: a baby with a family, on its own feet and not on a lead. */
	public static boolean isFamilyFoal(AbstractHorse horse) {
		return horse.isBaby() && horse.hasAttached(DATA) && !horse.isLeashed() && !horse.isVehicle()
				&& !horse.isPassenger() && HorseLoyaltyConfig.get().foalsEnabled();
	}
}
