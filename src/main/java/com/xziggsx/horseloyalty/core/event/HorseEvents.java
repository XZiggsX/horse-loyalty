package com.xziggsx.horseloyalty.core.event;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.feature.ListenerGates;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The hooks every feature listens to. Fired by the core mixins on {@code AbstractHorse},
 * {@code Animal} and {@code Horse}, so features never need a mixin of their own for these.
 * Features listen through their {@code FeatureContext}, which only lets a listener run while its
 * feature is on.
 */
public final class HorseEvents {
	/** Breeding phase: genetics crosses the parents' genes, which also decide the personality. */
	public static final Identifier GENES = id("genes");
	/** Breeding phase: traits the genes didn't set, such as personality's own inheritance. */
	public static final Identifier TRAITS = id("traits");
	/** Breeding phase: foals records the family and tames the foal. */
	public static final Identifier FAMILY = id("family");

	/** Server, every tick, for horses in scope. */
	public static final Event<ServerTick> SERVER_TICK = EventFactory.createArrayBacked(ServerTick.class,
			listeners -> (horse, level) -> {
				for (ServerTick listener : listeners) {
					listener.tick(horse, level);
				}
			});

	/** Server, once a second per horse in scope, spread over the 20 ticks by entity id. */
	public static final Event<ServerTick> SERVER_SECOND = EventFactory.createArrayBacked(ServerTick.class,
			listeners -> (horse, level) -> {
				for (ServerTick listener : listeners) {
					listener.tick(horse, level);
				}
			});

	/** Server: would this tamed horse take a treat it would normally refuse? Any true makes it eat. */
	public static final Event<WantsTreat> WANTS_TREAT = EventFactory.createArrayBacked(WantsTreat.class,
			listeners -> (horse, player, stack) -> {
				for (WantsTreat listener : listeners) {
					if (listener.wantsTreat(horse, player, stack)) {
						return true;
					}
				}
				return false;
			});

	/** Server: a tamed horse refused food or a treat from this player's hand, because nothing wanted it to eat. */
	public static final Event<Refused> REFUSED = EventFactory.createArrayBacked(Refused.class,
			listeners -> (horse, player, stack) -> {
				for (Refused listener : listeners) {
					listener.refused(horse, player, stack);
				}
			});

	/** Server: a tamed horse ate from this player's hand. */
	public static final Event<Ate> ATE = EventFactory.createArrayBacked(Ate.class,
			listeners -> (horse, player, stack) -> {
				for (Ate listener : listeners) {
					listener.ate(horse, player, stack);
				}
			});

	/** Server: a player mounted a tamed horse. */
	public static final Event<Mounted> MOUNTED = EventFactory.createArrayBacked(Mounted.class,
			listeners -> (horse, player) -> {
				for (Mounted listener : listeners) {
					listener.mounted(horse, player);
				}
			});

	/** Both sides, while any horse is built: add goals. Use {@code FeatureContext.goal} rather than this. */
	public static final Event<Goals> REGISTER_GOALS = EventFactory.createArrayBacked(Goals.class,
			listeners -> (horse, goals) -> {
				for (Goals listener : listeners) {
					listener.addGoals(horse, goals);
				}
			});

	/**
	 * Server, before a bred foal enters the world. Runs in the phases {@link #GENES}, {@link #TRAITS}
	 * and {@link #FAMILY}, in that order; a feature may add its own phase.
	 */
	public static final Event<Bred> BRED = EventFactory.createWithPhases(Bred.class,
			listeners -> (foal, parentA, parentB, breeder) -> {
				for (Bred listener : listeners) {
					listener.bred(foal, parentA, parentB, breeder);
				}
			}, GENES, TRAITS, FAMILY, Event.DEFAULT_PHASE);

	/** Server, after a wild (not bred) horse rolled its vanilla coat. */
	public static final Event<WildSpawn> WILD_SPAWN = EventFactory.createArrayBacked(WildSpawn.class,
			listeners -> (horse, groupData) -> {
				for (WildSpawn listener : listeners) {
					listener.spawned(horse, groupData);
				}
			});

	/** A tick listener. */
	@FunctionalInterface
	public interface ServerTick {
		void tick(AbstractHorse horse, ServerLevel level);
	}

	/** A treat listener: true if the horse should eat this treat now. */
	@FunctionalInterface
	public interface WantsTreat {
		boolean wantsTreat(AbstractHorse horse, Player player, ItemStack stack);
	}

	/** A refusal listener. */
	@FunctionalInterface
	public interface Refused {
		void refused(AbstractHorse horse, Player player, ItemStack stack);
	}

	/** An eating listener. */
	@FunctionalInterface
	public interface Ate {
		void ate(AbstractHorse horse, Player player, ItemStack stack);
	}

	/** A mounting listener. */
	@FunctionalInterface
	public interface Mounted {
		void mounted(AbstractHorse horse, ServerPlayer player);
	}

	/** A goal listener. */
	@FunctionalInterface
	public interface Goals {
		void addGoals(AbstractHorse horse, GoalSelector goals);
	}

	/**
	 * A breeding listener. {@code parentA} ran the breed (the foal spawns at its position); the
	 * breeder is the player who bred them, or null if nobody online did.
	 */
	@FunctionalInterface
	public interface Bred {
		void bred(AbstractHorse foal, AbstractHorse parentA, AbstractHorse parentB, ServerPlayer breeder);
	}

	/** A wild spawn listener. {@code groupData} is shared by the whole spawn group, or null. */
	@FunctionalInterface
	public interface WildSpawn {
		void spawned(AbstractHorse horse, SpawnGroupData groupData);
	}

	static {
		ListenerGates.register(SERVER_TICK, (listener, isOn) -> (horse, level) -> {
			if (isOn.getAsBoolean()) {
				listener.tick(horse, level);
			}
		});
		ListenerGates.register(SERVER_SECOND, (listener, isOn) -> (horse, level) -> {
			if (isOn.getAsBoolean()) {
				listener.tick(horse, level);
			}
		});
		ListenerGates.register(WANTS_TREAT, (listener, isOn) -> (horse, player, stack) ->
				isOn.getAsBoolean() && listener.wantsTreat(horse, player, stack));
		ListenerGates.register(REFUSED, (listener, isOn) -> (horse, player, stack) -> {
			if (isOn.getAsBoolean()) {
				listener.refused(horse, player, stack);
			}
		});
		ListenerGates.register(ATE, (listener, isOn) -> (horse, player, stack) -> {
			if (isOn.getAsBoolean()) {
				listener.ate(horse, player, stack);
			}
		});
		ListenerGates.register(MOUNTED, (listener, isOn) -> (horse, player) -> {
			if (isOn.getAsBoolean()) {
				listener.mounted(horse, player);
			}
		});
		ListenerGates.register(REGISTER_GOALS, (listener, isOn) -> (horse, goals) -> {
			if (isOn.getAsBoolean()) {
				listener.addGoals(horse, goals);
			}
		});
		ListenerGates.register(BRED, (listener, isOn) -> (foal, parentA, parentB, breeder) -> {
			if (isOn.getAsBoolean()) {
				listener.bred(foal, parentA, parentB, breeder);
			}
		});
		ListenerGates.register(WILD_SPAWN, (listener, isOn) -> (horse, groupData) -> {
			if (isOn.getAsBoolean()) {
				listener.spawned(horse, groupData);
			}
		});
	}

	private HorseEvents() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, path);
	}
}
