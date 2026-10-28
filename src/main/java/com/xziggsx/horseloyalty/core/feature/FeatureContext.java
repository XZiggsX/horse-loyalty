package com.xziggsx.horseloyalty.core.feature;

import com.xziggsx.horseloyalty.core.link.Contributions;
import com.xziggsx.horseloyalty.core.link.LinkKey;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What a feature hooks in with. Everything added here only runs while the feature is on, so
 * feature code never checks its own switch, and a switch can change while the game runs.
 * Get one from {@link Features#context}; {@link Features#wireAll} hands each feature its own.
 */
public interface FeatureContext {
	/** The id of the feature this context belongs to. */
	String featureId();

	/** Listens to an event (one of {@code HorseEvents}, or any Fabric event) while the feature is on. */
	<T> void on(Event<T> event, T listener);

	/** As {@link #on(Event, Object)}, in one of the event's phases, such as a breeding phase. */
	<T> void on(Event<T> event, Identifier phase, T listener);

	/** Handles a packet from a client while the feature is on. While it's off the packet is ignored. */
	<T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type, ServerPlayNetworking.PlayPayloadHandler<T> handler);

	/** Provides a link to other features. While this feature is off, the key's neutral stand-in answers instead. */
	<T> void provide(LinkKey<T> key, T implementation);

	/** Adds an item to another feature's list, such as a ride modifier. It only counts while this feature is on. */
	<T> void contribute(Contributions<T> list, T item);
}
