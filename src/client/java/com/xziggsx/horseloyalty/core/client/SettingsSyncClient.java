package com.xziggsx.horseloyalty.core.client;

import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.SettingsSync;
import com.xziggsx.horseloyalty.core.feature.Features;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client side of {@link SettingsSync}: follows the server's switches and synced sections while
 * connected to it, and this side's own config again after disconnecting.
 */
public final class SettingsSyncClient {
	private SettingsSyncClient() {
	}

	/** Called once from the client entrypoint. */
	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(SettingsSync.Payload.TYPE, (payload, context) -> {
			ConfigFile.useRemote(payload.sections());
			Features.useRemote(payload.featureSwitches());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ConfigFile.useRemote(null);
			Features.useRemote(null);
		});
	}
}
