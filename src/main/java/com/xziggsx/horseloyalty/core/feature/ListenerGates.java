package com.xziggsx.horseloyalty.core.feature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

/**
 * Wraps event listeners so they only run while their feature is on. An event can register a
 * {@link Gate} that knows its listener type (fast, and gives the right "do nothing" answer); any
 * other event gets a generic wrapper that answers false, 0, PASS or null while the feature is off.
 */
public final class ListenerGates {
	/** Wraps one listener of an event so it only runs while {@code isOn} says so. */
	@FunctionalInterface
	public interface Gate<T> {
		T wrap(T listener, BooleanSupplier isOn);
	}

	private static final Map<Event<?>, Gate<?>> GATES = new IdentityHashMap<>();

	static {
		register(UseEntityCallback.EVENT, (listener, isOn) -> (player, level, hand, entity, hit) ->
				isOn.getAsBoolean() ? listener.interact(player, level, hand, entity, hit) : InteractionResult.PASS);
	}

	private ListenerGates() {
	}

	/** Sets how listeners of this event are gated. Call it where the event is created. */
	public static synchronized <T> void register(Event<T> event, Gate<T> gate) {
		GATES.put(event, gate);
	}

	/** The listener, wrapped so it only runs while {@code isOn} says so. */
	@SuppressWarnings("unchecked")
	static synchronized <T> T wrap(Event<T> event, T listener, BooleanSupplier isOn) {
		Gate<T> gate = (Gate<T>) GATES.get(event);
		return gate != null ? gate.wrap(listener, isOn) : proxy(listener, isOn);
	}

	/** The generic wrapper, for events without their own gate. */
	@SuppressWarnings("unchecked")
	private static <T> T proxy(T listener, BooleanSupplier isOn) {
		Class<?>[] interfaces = listener.getClass().getInterfaces();
		return (T) Proxy.newProxyInstance(listener.getClass().getClassLoader(), interfaces, (self, method, args) -> {
			if (method.getDeclaringClass() == Object.class) {
				return method.invoke(listener, args);
			}
			if (!isOn.getAsBoolean()) {
				return offAnswer(method);
			}
			try {
				return method.invoke(listener, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		});
	}

	/** What a listener answers while its feature is off: nothing changes. */
	private static Object offAnswer(Method method) {
		Class<?> type = method.getReturnType();
		if (type == boolean.class) {
			return false;
		}
		if (type == InteractionResult.class) {
			return InteractionResult.PASS;
		}
		if (type == int.class) {
			return 0;
		}
		if (type == long.class) {
			return 0L;
		}
		if (type == float.class) {
			return 0.0F;
		}
		if (type == double.class) {
			return 0.0;
		}
		return null;
	}
}
