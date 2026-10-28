package com.xziggsx.horseloyalty.config;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.personality.Personality;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The config values that riding uses, as resolved by {@link HorseLoyaltyConfig}. Riding runs on the
 * rider's client, so without this a player could edit their own config to ride faster or never
 * have a gait drop or a horse refuse. Instead the server sends its rules to every player who joins
 * (see {@link Payload}), and the client rides by them until it disconnects. A server without the
 * mod sends nothing, and the client's own config applies. Arrays are indexed like the config's:
 * by loyalty level 0 to 5, or by gait 0 (halt) to 4 (gallop).
 * <p>
 * Also carries whether genetics is on, so the horse info screen follows the server, not the
 * player's own file.
 */
public record RidingRules(
		double[] topSpeedShare,
		double[] gaitShares,
		float[] turnDegrees,
		int turnEaseTicks,
		double sidestepAngleDegrees,
		double[] gaitDropChance,
		int[] gaitDropTicks,
		boolean personalityEnabled,
		double lazyGaitDropChanceMultiplier,
		double lazyGaitDropTimeMultiplier,
		double boltSeconds,
		double boltCreeperDistance,
		boolean gaitRaiseEnabled,
		int gaitRaiseCooldownTicks,
		double[][] gaitRaiseFailChance,
		List<Personality> buckOffPersonalities,
		double buckOffChance,
		boolean gallopPushEnabled,
		double gallopPushStep,
		double gallopPushMax,
		int gallopPushHoldTicks,
		double gallopPushFadePerTick,
		boolean geneticsEnabled) {

	public static final StreamCodec<ByteBuf, RidingRules> STREAM_CODEC = StreamCodec.of(RidingRules::write, RidingRules::read);

	/** Server to client, on joining: the rules to ride by on this server. */
	public record Payload(RidingRules rules) implements CustomPacketPayload {
		public static final Type<Payload> TYPE =
				new Type<>(Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "riding_rules"));

		public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC =
				RidingRules.STREAM_CODEC.map(Payload::new, Payload::rules);

		@Override
		public Type<Payload> type() {
			return TYPE;
		}
	}

	/** Registers the packet, and sends the server's rules to every player that joins. Called once from the common entrypoint. */
	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> {
			if (ServerPlayNetworking.canSend(handler.getPlayer(), Payload.TYPE)) {
				ServerPlayNetworking.send(handler.getPlayer(), new Payload(HorseLoyaltyConfig.localRidingRules()));
			}
		});
	}

	private static void write(ByteBuf buf, RidingRules rules) {
		writeDoubles(buf, rules.topSpeedShare);
		writeDoubles(buf, rules.gaitShares);
		buf.writeInt(rules.turnDegrees.length);
		for (float value : rules.turnDegrees) {
			buf.writeFloat(value);
		}
		buf.writeInt(rules.turnEaseTicks);
		buf.writeDouble(rules.sidestepAngleDegrees);
		writeDoubles(buf, rules.gaitDropChance);
		buf.writeInt(rules.gaitDropTicks.length);
		for (int value : rules.gaitDropTicks) {
			buf.writeInt(value);
		}
		buf.writeBoolean(rules.personalityEnabled);
		buf.writeDouble(rules.lazyGaitDropChanceMultiplier);
		buf.writeDouble(rules.lazyGaitDropTimeMultiplier);
		buf.writeDouble(rules.boltSeconds);
		buf.writeDouble(rules.boltCreeperDistance);
		buf.writeBoolean(rules.gaitRaiseEnabled);
		buf.writeInt(rules.gaitRaiseCooldownTicks);
		buf.writeInt(rules.gaitRaiseFailChance.length);
		for (double[] byLevel : rules.gaitRaiseFailChance) {
			writeDoubles(buf, byLevel);
		}
		buf.writeInt(rules.buckOffPersonalities.size());
		for (Personality personality : rules.buckOffPersonalities) {
			buf.writeInt(personality.ordinal());
		}
		buf.writeDouble(rules.buckOffChance);
		buf.writeBoolean(rules.gallopPushEnabled);
		buf.writeDouble(rules.gallopPushStep);
		buf.writeDouble(rules.gallopPushMax);
		buf.writeInt(rules.gallopPushHoldTicks);
		buf.writeDouble(rules.gallopPushFadePerTick);
		buf.writeBoolean(rules.geneticsEnabled);
	}

	private static RidingRules read(ByteBuf buf) {
		double[] topSpeedShare = readDoubles(buf);
		double[] gaitShares = readDoubles(buf);
		float[] turnDegrees = new float[readLength(buf)];
		for (int i = 0; i < turnDegrees.length; i++) {
			turnDegrees[i] = buf.readFloat();
		}
		int turnEaseTicks = buf.readInt();
		double sidestepAngleDegrees = buf.readDouble();
		double[] gaitDropChance = readDoubles(buf);
		int[] gaitDropTicks = new int[readLength(buf)];
		for (int i = 0; i < gaitDropTicks.length; i++) {
			gaitDropTicks[i] = buf.readInt();
		}
		boolean personalityEnabled = buf.readBoolean();
		double lazyChance = buf.readDouble();
		double lazyTime = buf.readDouble();
		double boltSeconds = buf.readDouble();
		double boltDistance = buf.readDouble();
		boolean raiseEnabled = buf.readBoolean();
		int raiseCooldown = buf.readInt();
		double[][] failChance = new double[readLength(buf)][];
		for (int i = 0; i < failChance.length; i++) {
			failChance[i] = readDoubles(buf);
		}
		Personality[] personalities = Personality.values();
		List<Personality> buckOff = new ArrayList<>();
		for (int i = readLength(buf); i > 0; i--) {
			buckOff.add(personalities[Math.clamp(buf.readInt(), 0, personalities.length - 1)]);
		}
		return new RidingRules(topSpeedShare, gaitShares, turnDegrees, turnEaseTicks, sidestepAngleDegrees,
				gaitDropChance, gaitDropTicks, personalityEnabled, lazyChance, lazyTime, boltSeconds, boltDistance,
				raiseEnabled, raiseCooldown, failChance, buckOff, buf.readDouble(), buf.readBoolean(),
				buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readDouble(), buf.readBoolean());
	}

	private static void writeDoubles(ByteBuf buf, double[] values) {
		buf.writeInt(values.length);
		for (double value : values) {
			buf.writeDouble(value);
		}
	}

	private static double[] readDoubles(ByteBuf buf) {
		double[] values = new double[readLength(buf)];
		for (int i = 0; i < values.length; i++) {
			values[i] = buf.readDouble();
		}
		return values;
	}

	/** An array length from the server, kept sane so a bad packet can't allocate huge arrays. */
	private static int readLength(ByteBuf buf) {
		return Math.clamp(buf.readInt(), 0, 16);
	}
}
