package com.xziggsx.horseloyalty.core.config;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.feature.FeatureSwitch;
import com.xziggsx.horseloyalty.core.feature.Features;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends the server's feature switches and its {@link Synced} sections to every player. Riding runs
 * on the rider's client, so without this a player could edit their own config to ride faster or
 * switch a feature back on. The client follows what it gets until it disconnects (see the client's
 * {@code SettingsSyncClient}); a server without the mod sends nothing, and the client's own config
 * applies. Each section travels as the same JSON as in the file, so a new setting needs no packet code.
 */
public final class SettingsSync {
	/**
	 * Server to client: the switches, as "on" or "off" by feature id, and each synced section as JSON by key.
	 */
	public record Payload(Map<String, String> switches, Map<String, String> sections) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "settings"));

		private static final StreamCodec<ByteBuf, Map<String, String>> MAP_CODEC =
				ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.stringUtf8(1 << 16), 64);

		public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
				MAP_CODEC, Payload::switches,
				MAP_CODEC, Payload::sections,
				Payload::new);

		@Override
		public Type<Payload> type() {
			return TYPE;
		}

		/** The switches as switch values; anything unreadable counts as off. */
		public Map<String, FeatureSwitch> featureSwitches() {
			Map<String, FeatureSwitch> result = new HashMap<>();
			switches.forEach((id, value) -> {
				FeatureSwitch parsed = FeatureSwitch.byId(value);
				result.put(id, parsed != null ? parsed : FeatureSwitch.OFF);
			});
			return result;
		}
	}

	private SettingsSync() {
	}

	/**
	 * Registers the packet, sends it to every player who joins, and reads the world's config
	 * override while a server runs. Called once from the common entrypoint.
	 */
	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STARTING.register(ConfigFile::useWorld);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ConfigFile.leaveWorld());
	}

	/** Sends the current switches and sections to every player, after a switch or the config changed. */
	public static void sendToAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			send(player);
			// Commands show or hide with their feature, so players get the new command tree too.
			server.getCommands().sendCommands(player);
		}
	}

	private static void send(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, Payload.TYPE)) {
			ServerPlayNetworking.send(player, new Payload(switchIds(), ConfigFile.syncedSections()));
		}
	}

	private static Map<String, String> switchIds() {
		Map<String, String> ids = new HashMap<>();
		Features.localSwitches().forEach((id, value) -> ids.put(id, value.id()));
		return ids;
	}
}
