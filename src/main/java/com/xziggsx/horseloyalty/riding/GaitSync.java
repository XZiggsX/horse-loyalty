package com.xziggsx.horseloyalty.riding;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.GaitEvent;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * Shares a ridden horse's gait with the server and every other player.
 * <p>
 * Riding runs on the rider's client (see {@code AbstractHorseRidingMixin}), so only that client
 * knows the gait. It sends a {@link GaitPayload} whenever the gait changes or the horse drops a
 * gait, bolts or balks at a shift up. The server keeps the gait in {@link #GAIT}, which is synced to
 * everyone, so other clients can show gait dust and every side can pick the right hoof sound. Those
 * events also play the horse's angry sound for everyone but the rider, who already heard it on their
 * own client. A balking horse rears up on the server, and one that bucks throws its rider off there.
 */
public final class GaitSync {
	/** At most one drop or bolt sound per horse this often, whatever a client sends. */
	private static final int SOUND_COOLDOWN_TICKS = 10;

	/** Gaits are sent as their index: 0 halt, 1 walk, 2 trot, 3 canter, 4 gallop. */
	private static final StreamCodec<ByteBuf, Gait> GAIT_CODEC = ByteBufCodecs.VAR_INT.map(Gait::byIndex, Gait::ordinal);

	/** The ridden horse's gait. Not saved; removed when nobody rides the horse. Synced to all. */
	public static final AttachmentType<Gait> GAIT = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "gait"),
			builder -> builder.syncWith(GAIT_CODEC, AttachmentSyncPredicate.all())
	);

	/** Game time of the last drop or bolt sound, for the cooldown. Server only, not saved. */
	private static final AttachmentType<Long> LAST_SOUND_TICK = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "last_gait_sound")
	);

	/** Rider's client to server: the horse the player is riding is now at this gait. */
	public record GaitPayload(Gait gait, GaitEvent event) implements CustomPacketPayload {
		public static final Type<GaitPayload> TYPE =
				new Type<>(Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "gait"));

		public static final StreamCodec<ByteBuf, GaitPayload> STREAM_CODEC = StreamCodec.composite(
				GAIT_CODEC, GaitPayload::gait,
				GaitEvent.STREAM_CODEC, GaitPayload::event,
				GaitPayload::new);

		@Override
		public Type<GaitPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Implemented on every horse by {@code AbstractHorseMixin}, so the packet handler can play the
	 * horse's own angry sound (it differs for donkeys and the undead horses).
	 */
	public interface HorseSounds {
		SoundEvent horseloyalty$getAngrySound();
	}

	private GaitSync() {
	}

	/** Registers the attachment (by loading the class) and the packet type. Runs whether riding is on or off. */
	public static void registerData() {
		PayloadTypeRegistry.serverboundPlay().register(GaitPayload.TYPE, GaitPayload.STREAM_CODEC);
	}

	/** Handles the riders' gait packets while riding is on. */
	public static void wire(FeatureContext context) {
		context.receive(GaitPayload.TYPE, (payload, packetContext) -> receive(payload, packetContext.player()));
	}

	/** The horse's synced gait, or null if nobody with the mod is riding it. Works on both sides. */
	public static Gait gait(AbstractHorse horse) {
		return horse.getAttached(GAIT);
	}

	/** Server: a rider's client reported its gait. Ignored unless they're steering a horse this mod handles. */
	private static void receive(GaitPayload payload, ServerPlayer player) {
		if (!(player.getVehicle() instanceof AbstractHorse horse) || horse.getControllingPassenger() != player
				|| !HorseScope.inScope(horse)) {
			return;
		}
		Gait gait = payload.gait();
		if (gait(horse) != gait) {
			horse.setAttached(GAIT, gait);
		}
		if (payload.event() == GaitEvent.SHIFT) {
			return;
		}
		if (payload.event() == GaitEvent.PRANCED || payload.event() == GaitEvent.BUCKED) {
			// Rearing is synced to every client, and stops the horse for its rider too.
			horse.standIfPossible();
			if (payload.event() == GaitEvent.BUCKED) {
				player.stopRiding();
			}
		}
		long now = horse.level().getGameTime();
		Long last = horse.getAttached(LAST_SOUND_TICK);
		if (last != null && now - last < SOUND_COOLDOWN_TICKS) {
			return;
		}
		horse.setAttached(LAST_SOUND_TICK, now);
		SoundEvent angry = ((HorseSounds) horse).horseloyalty$getAngrySound();
		if (angry != null) {
			// Everyone nearby but the rider, who already heard it on their own client.
			horse.level().playSound(player, horse, angry, horse.getSoundSource(), 1.0F, horse.getVoicePitch());
		}
	}

	/** Server, every tick: forgets the gait once no player is steering the horse. */
	public static void tick(AbstractHorse horse) {
		if (horse.hasAttached(GAIT) && horse.getControllingPassenger() == null) {
			horse.removeAttached(GAIT);
		}
	}
}
