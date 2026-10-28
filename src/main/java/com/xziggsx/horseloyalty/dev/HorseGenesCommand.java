package com.xziggsx.horseloyalty.dev;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.genetics.core.CrossPredictor;
import com.xziggsx.horseloyalty.genetics.core.Dice;
import com.xziggsx.horseloyalty.genetics.core.Distribution;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Inheritance;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.genetics.view.GeneticsView;
import com.xziggsx.horseloyalty.genetics.view.ShownTrait;
import com.xziggsx.horseloyalty.genetics.world.HorseGenetics;
import com.xziggsx.horseloyalty.genetics.world.Lineage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Operator-only {@code /horsegenes} for testing genetics. Acts on the horse you look at
 * (up to {@link #RANGE} blocks away):
 * <ul>
 *   <li>{@code show}: its genes, what it shows, and its parents' genes</li>
 *   <li>{@code set <locus> <v1> <v2>}: changes one gene pair and makes the horse show its genes again</li>
 *   <li>{@code mark}: remembers it as horse A for predict and simulate</li>
 *   <li>{@code predict}: the exact odds for a foal of the marked horse and this one</li>
 *   <li>{@code simulate <count>}: breeds that many foals on paper and compares the counts with the odds</li>
 * </ul>
 */
public final class HorseGenesCommand {
	private static final double RANGE = 8;
	private static final int MAX_SIMULATIONS = 100_000;

	/** The horse each player marked as horse A, by player. Forgotten on restart. */
	private static final Map<UUID, UUID> MARKED = new HashMap<>();

	private static final SuggestionProvider<CommandSourceStack> LOCI = (context, builder) ->
			SharedSuggestionProvider.suggest(Loci.ALL.stream().map(Locus::id), builder);

	private static final SuggestionProvider<CommandSourceStack> VERSIONS = (context, builder) -> {
		Locus<?> locus;
		try {
			locus = Loci.byId(StringArgumentType.getString(context, "locus"));
		} catch (IllegalArgumentException e) {
			locus = null;
		}
		if (locus == null) {
			return builder.buildFuture();
		}
		return SharedSuggestionProvider.suggest(locus.versions().stream().map(Locus::name), builder);
	};

	private HorseGenesCommand() {
	}

	/** Registers the command while genetics is on. */
	public static void init(FeatureContext context) {
		context.on(CommandRegistrationCallback.EVENT, (dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("horsegenes")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("show").executes(HorseGenesCommand::show))
				.then(Commands.literal("set")
						.then(Commands.argument("locus", StringArgumentType.word()).suggests(LOCI)
								.then(Commands.argument("v1", StringArgumentType.word()).suggests(VERSIONS)
										.then(Commands.argument("v2", StringArgumentType.word()).suggests(VERSIONS)
												.executes(HorseGenesCommand::set)))))
				.then(Commands.literal("mark").executes(HorseGenesCommand::mark))
				.then(Commands.literal("predict").executes(HorseGenesCommand::predict))
				.then(Commands.literal("simulate")
						.then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SIMULATIONS))
								.executes(HorseGenesCommand::simulate))));
	}

	// --- Subcommands -------------------------------------------------------------

	private static int show(CommandContext<CommandSourceStack> context) {
		AbstractHorse horse = target(context.getSource());
		if (horse == null) {
			return 0;
		}
		Genome genome = HorseGenetics.genomeOf(horse);
		List<String> lines = new ArrayList<>();
		lines.add("Genes of " + horse.getName().getString() + ":");
		genome.pairs().forEach((locus, pair) -> lines.add("  " + locus.id() + ": " + pair));
		lines.add("Shows: " + shown(horse));
		Optional<Lineage> lineage = GeneticsView.lineage(horse);
		if (lineage.isEmpty() || !lineage.get().hasParents()) {
			lines.add("Parents: not recorded (older horse)");
		} else {
			lines.add("Parent A" + named(lineage.get().nameA()) + ": " + lineage.get().parentA().map(Genome::toString).orElse("unknown"));
			lines.add("Parent B" + named(lineage.get().nameB()) + ": " + lineage.get().parentB().map(Genome::toString).orElse("unknown"));
		}
		reply(context, lines);
		return 1;
	}

	private static int set(CommandContext<CommandSourceStack> context) {
		AbstractHorse horse = target(context.getSource());
		if (horse == null) {
			return 0;
		}
		String locusId = StringArgumentType.getString(context, "locus");
		Locus<?> locus = Loci.byId(locusId);
		if (locus == null) {
			context.getSource().sendFailure(Component.literal("No locus called " + locusId + "."));
			return 0;
		}
		Genome changed = withPair(HorseGenetics.genomeOf(horse), locus,
				StringArgumentType.getString(context, "v1"), StringArgumentType.getString(context, "v2"));
		if (changed == null) {
			List<String> names = locus.versions().stream().map(Locus::name).toList();
			context.getSource().sendFailure(Component.literal(locus.id() + " versions are: " + String.join(", ", names) + "."));
			return 0;
		}
		HorseGenetics.setGenome(horse, changed);
		reply(context, List.of("Set " + locus.id() + " to " + changed.get(locus) + ". Now shows: " + shown(horse)));
		return 1;
	}

	private static int mark(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = context.getSource().getPlayer();
		AbstractHorse horse = target(context.getSource());
		if (horse == null || player == null) {
			return 0;
		}
		HorseGenetics.genomeOf(horse);
		MARKED.put(player.getUUID(), horse.getUUID());
		reply(context, List.of("Marked " + horse.getName().getString() + " as horse A. Look at another horse and use predict or simulate."));
		return 1;
	}

	private static int predict(CommandContext<CommandSourceStack> context) {
		AbstractHorse a = marked(context.getSource());
		AbstractHorse b = a == null ? null : target(context.getSource());
		if (b == null) {
			return 0;
		}
		Map<Trait<?>, Distribution<?>> odds = CrossPredictor.predictAll(HorseGenetics.genomeOf(a), HorseGenetics.genomeOf(b), HorseTraits.ALL,
				HorseLoyaltyConfig.get().geneticsMutationChance());
		List<String> lines = new ArrayList<>();
		lines.add("Foal of " + a.getName().getString() + " and " + b.getName().getString() + ":");
		odds.forEach((trait, distribution) -> lines.add("  " + trait.id() + ": " + percentages(distribution)));
		addSameHorseNote(lines, a, b);
		reply(context, lines);
		return 1;
	}

	private static int simulate(CommandContext<CommandSourceStack> context) {
		AbstractHorse a = marked(context.getSource());
		AbstractHorse b = a == null ? null : target(context.getSource());
		if (b == null) {
			return 0;
		}
		int count = IntegerArgumentType.getInteger(context, "count");
		Genome genomeA = HorseGenetics.genomeOf(a);
		Genome genomeB = HorseGenetics.genomeOf(b);
		double mutation = HorseLoyaltyConfig.get().geneticsMutationChance();
		Dice dice = HorseGenetics.dice(context.getSource().getLevel().getRandom());

		Map<Trait<?>, Map<Object, Integer>> counts = new LinkedHashMap<>();
		for (Trait<?> trait : HorseTraits.ALL) {
			counts.put(trait, new LinkedHashMap<>());
		}
		for (int i = 0; i < count; i++) {
			Genome foal = Inheritance.cross(genomeA, genomeB, mutation, dice);
			for (Trait<?> trait : HorseTraits.ALL) {
				counts.get(trait).merge(trait.express(foal).sample(dice), 1, Integer::sum);
			}
		}

		List<String> lines = new ArrayList<>();
		lines.add(count + " foals of " + a.getName().getString() + " and " + b.getName().getString() + " (count, then predicted):");
		for (Trait<?> trait : HorseTraits.ALL) {
			Map<Object, Integer> byValue = counts.get(trait);
			Distribution<?> predicted = CrossPredictor.predict(genomeA, genomeB, trait, mutation);
			// Also list values the prediction allows that never came up.
			predicted.outcomes().keySet().forEach(value -> byValue.putIfAbsent(value, 0));
			lines.add("  " + trait.id() + ":");
			byValue.entrySet().stream()
					.sorted((x, y) -> Integer.compare(y.getValue(), x.getValue()))
					.forEach(entry -> lines.add("    " + name(entry.getKey()) + ": " + entry.getValue()
							+ " (" + percent(entry.getValue() / (double) count) + "), predicted "
							+ percent(chanceOf(predicted, entry.getKey()))));
		}
		addSameHorseNote(lines, a, b);
		reply(context, lines);
		return 1;
	}

	// --- Helpers -----------------------------------------------------------------

	/** The horse the player is looking at, or null with a message saying why not. */
	private static AbstractHorse target(CommandSourceStack source) {
		if (!HorseGenetics.enabled()) {
			source.sendFailure(Component.literal("Genetics is turned off in the config."));
			return null;
		}
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Only a player can use this: it acts on the horse you look at."));
			return null;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 reach = player.getViewVector(1.0F).scale(RANGE);
		AABB area = player.getBoundingBox().expandTowards(reach).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach), area,
				entity -> entity instanceof AbstractHorse horse && horse.isAlive() && HorseScope.inScope(horse), RANGE * RANGE);
		if (hit == null || !(hit.getEntity() instanceof AbstractHorse horse)) {
			source.sendFailure(Component.literal("Look at a horse, donkey or mule within " + (int) RANGE + " blocks."));
			return null;
		}
		return horse;
	}

	/** The player's marked horse A, or null with a message saying why not. */
	private static AbstractHorse marked(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		UUID id = player == null ? null : MARKED.get(player.getUUID());
		if (id == null) {
			source.sendFailure(Component.literal("Mark a horse first: look at it and use /horsegenes mark."));
			return null;
		}
		if (!(source.getLevel().getEntity(id) instanceof AbstractHorse horse) || !horse.isAlive()) {
			source.sendFailure(Component.literal("The marked horse isn't loaded here any more. Mark one again."));
			return null;
		}
		return horse;
	}

	/** A copy of the genome with the locus set to the two named versions, or null if a name is wrong. */
	private static <A extends Enum<A>> Genome withPair(Genome genome, Locus<A> locus, String first, String second) {
		A a = locus.parse(first);
		A b = locus.parse(second);
		return a == null || b == null ? null : genome.with(locus, new GenePair<>(a, b));
	}

	/** What the horse shows now for each trait it has, such as "coat brown, marking none". */
	private static String shown(AbstractHorse horse) {
		List<String> parts = new ArrayList<>();
		for (ShownTrait<?> shown : GeneticsView.shownTraits(horse)) {
			parts.add(shown.trait().id() + " " + name(shown.current()));
		}
		return String.join(", ", parts);
	}

	private static String percentages(Distribution<?> distribution) {
		List<String> parts = new ArrayList<>();
		distribution.outcomes().entrySet().stream()
				.sorted((x, y) -> Double.compare(y.getValue(), x.getValue()))
				.forEach(entry -> parts.add(name(entry.getKey()) + " " + percent(entry.getValue())));
		return String.join(", ", parts);
	}

	@SuppressWarnings("unchecked")
	private static double chanceOf(Distribution<?> distribution, Object value) {
		return ((Distribution<Object>) distribution).chance(value);
	}

	private static String named(Optional<String> name) {
		return " (" + name.orElse("unnamed") + ")";
	}

	/** A share as a percentage with one decimal, such as "12.5%". Possible but tiny chances read "<0.1%", not "0.0%". */
	private static String percent(double share) {
		return share > 0 && share < 0.0005 ? "<0.1%" : String.format(Locale.ROOT, "%.1f%%", share * 100);
	}

	/** A note for predict and simulate when horse A is the horse being looked at. */
	private static void addSameHorseNote(List<String> lines, AbstractHorse a, AbstractHorse b) {
		if (a.getUUID().equals(b.getUUID())) {
			lines.add("(same horse as A and B; horses can't breed with themselves in game)");
		}
	}

	private static String name(Object value) {
		return value instanceof Enum<?> version ? Locus.name(version) : String.valueOf(value);
	}

	private static void reply(CommandContext<CommandSourceStack> context, List<String> lines) {
		context.getSource().sendSuccess(() -> Component.literal(String.join("\n", lines)), false);
	}
}
