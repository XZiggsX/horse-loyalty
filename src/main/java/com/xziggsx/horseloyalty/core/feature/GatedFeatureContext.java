package com.xziggsx.horseloyalty.core.feature;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The {@link FeatureContext} of one feature: wraps everything in a check of that feature's switch. */
final class GatedFeatureContext implements FeatureContext {
	private final String featureId;

	GatedFeatureContext(String featureId) {
		this.featureId = featureId;
	}

	@Override
	public String featureId() {
		return featureId;
	}

	/** Whether this context's feature is on right now. */
	boolean isOn() {
		return Features.isOn(featureId);
	}

	@Override
	public <T> void on(Event<T> event, T listener) {
		event.register(ListenerGates.wrap(event, listener, this::isOn));
	}

	@Override
	public <T> void on(Event<T> event, Identifier phase, T listener) {
		event.register(phase, ListenerGates.wrap(event, listener, this::isOn));
	}

	@Override
	public <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type,
			ServerPlayNetworking.PlayPayloadHandler<T> handler) {
		ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
			if (isOn()) {
				handler.receive(payload, context);
			}
		});
	}
}
