package com.xziggsx.horseloyalty.core.feature;

import com.xziggsx.horseloyalty.core.event.HorseEvents;
import com.xziggsx.horseloyalty.core.link.Contributions;
import com.xziggsx.horseloyalty.core.link.LinkKey;
import com.xziggsx.horseloyalty.core.link.Links;
import java.util.function.Function;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

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
	public void goal(int priority, Function<AbstractHorse, Goal> goal) {
		// Not gated: goals are added when the horse is built; the goal itself checks the switch.
		HorseEvents.REGISTER_GOALS.register((horse, goals) -> goals.addGoal(priority, new GatedGoal(goal.apply(horse), this::isOn)));
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

	@Override
	public <T> void provide(LinkKey<T> key, T implementation) {
		Links.provide(featureId, key, implementation);
	}

	@Override
	public <T> void contribute(Contributions<T> list, T item) {
		list.add(featureId, item);
	}
}
