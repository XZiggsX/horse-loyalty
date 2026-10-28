package com.xziggsx.horseloyalty.genetics.dev;

import com.xziggsx.horseloyalty.core.command.HorseCommands;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.genetics.GeneticsSettings;
import com.xziggsx.horseloyalty.genetics.core.CrossPredictor;
import com.xziggsx.horseloyalty.genetics.core.Dice;
import com.xziggsx.horseloyalty.genetics.core.Distribution;
import com.xziggsx.horseloyalty.genetics.core.GenePair;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.GenomeFactory;
import com.xziggsx.horseloyalty.genetics.core.Inheritance;
import com.xziggsx.horseloyalty.genetics.core.Loci;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.core.Trait;
import com.xziggsx.horseloyalty.genetics.trait.HorseTraits;
import com.xziggsx.horseloyalty.genetics.view.GeneticsView;
import com.xziggsx.horseloyalty.genetics.view.ShownTrait;
import com.xziggsx.horseloyalty.genetics.world.HorseGenetics;
import com.xziggsx.horseloyalty.genetics.world.Lineage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;

/**
 * {@code /hl genes} for testing genetics, also reachable as {@code /horsegenes}. Acts on the horse
 * you look at (or a selector at the end):
 * <ul>
 *   <li>{@code show}: its genes, what it shows, and its parents' genes</li>
 *   <li>{@code set <locus> <v1> <v2>}: changes one gene pair and makes the horse show its genes again</li>
 *   <li>{@code look <coat> [marking]}: gives it genes that show this look, with random hidden copies</li>
 *   <li>{@code mark}: remembers it as horse A for predict, simulate and {@code /hl foal breed}</li>
 *   <li>{@code predict}: the exact odds for a foal of the marked horse and this one</li>
 *   <li>{@code simulate <count>}: breeds that many foals on paper and compares the counts with the odds</li>
 *   <li>{@code lineage}: its parents' names and genomes, as recorded at birth (operators, without dev tools)</li>
 * </ul>
 */
public final class GeneticsCommands {
	private static final int MAX_SIMULATIONS = 100_000;

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

	private static final SuggestionProvider<CommandSourceStack> COATS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(Variant.values()).map(GeneticsCommands::id), builder);

	private static final SuggestionProvider<CommandSourceStack> MARKINGS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(Markings.values()).map(GeneticsCommands::id), builder);

	private GeneticsCommands() {
	}

	/** Adds the commands, and {@code /horsegenes} as an alias. */
	public static void register(FeatureContext context) {
		context.devCommand("genes", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("show"), GeneticsCommands::show))
				.then(Commands.literal("set")
						.then(Commands.argument("locus", StringArgumentType.word()).suggests(LOCI)
								.then(Commands.argument("v1", StringArgumentType.word()).suggests(VERSIONS)
										.then(HorseCommands.withHorse(Commands.argument("v2", StringArgumentType.word()).suggests(VERSIONS),
												GeneticsCommands::set)))))
				.then(Commands.literal("look")
						.then(HorseCommands.withHorse(Commands.argument("coat", StringArgumentType.word()).suggests(COATS), GeneticsCommands::look)
								.then(HorseCommands.withHorse(Commands.argument("marking", StringArgumentType.word()).suggests(MARKINGS),
										GeneticsCommands::look))))
				.then(HorseCommands.withHorse(Commands.literal("mark"), GeneticsCommands::mark))
				.then(HorseCommands.withHorse(Commands.literal("predict"), GeneticsCommands::predict))
				.then(Commands.literal("simulate")
						.then(HorseCommands.withHorse(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SIMULATIONS)),
								GeneticsCommands::simulate))));
		context.adminCommand("genes", branch -> branch
				.then(HorseCommands.withHorse(Commands.literal("lineage"), GeneticsCommands::lineage)));
		HorseCommands.addAlias("horsegenes", "genes");
	}

	// --- Subcommands -------------------------------------------------------------

	private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		Genome genome = HorseGenetics.genomeOf(horse);
		List<String> lines = new ArrayList<>();
		lines.add("Genes of " + horse.getName().getString() + ":");
		genome.pairs().forEach((locus, pair) -> lines.add("  " + locus.id() + ": " + pair));
		lines.add("Shows: " + shown(horse));
		addParents(lines, horse);
		return HorseCommands.replyLines(context, lines);
	}

	private static int lineage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		List<String> lines = new ArrayList<>();
		lines.add("Lineage of " + horse.getName().getString() + ":");
		addParents(lines, horse);
		return HorseCommands.replyLines(context, lines);
	}

	private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
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
		return HorseCommands.replyLines(context, List.of("Set " + locus.id() + " to " + changed.get(locus) + ". Now shows: " + shown(horse)));
	}

	/** Genes that show this coat (and marking), keeping the other genes; hidden copies are random. */
	private static int look(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		if (!(horse instanceof Horse)) {
			context.getSource().sendFailure(Component.literal("Only horses have coat and marking genes."));
			return 0;
		}
		Variant coat = parse(Variant.values(), StringArgumentType.getString(context, "coat"));
		Markings marking = HorseCommands.has(context, "marking")
				? parse(Markings.values(), StringArgumentType.getString(context, "marking")) : null;
		if (coat == null || HorseCommands.has(context, "marking") && marking == null) {
			context.getSource().sendFailure(Component.literal("Coats: " + names(Variant.values()) + ". Markings: " + names(Markings.values()) + "."));
			return 0;
		}
		Dice dice = HorseGenetics.dice(horse.getRandom());
		Genome genome = GenomeFactory.refit(HorseGenetics.genomeOf(horse), HorseTraits.COAT, coat, dice);
		if (marking != null) {
			genome = GenomeFactory.refit(genome, HorseTraits.MARKING, marking, dice);
		}
		HorseGenetics.setGenome(horse, genome);
		return HorseCommands.replyLines(context, List.of("Now shows: " + shown(horse)));
	}

	private static int mark(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		HorseGenetics.genomeOf(horse);
		HorseCommands.mark(context.getSource().getPlayerOrException(), horse);
		return HorseCommands.replyLines(context, List.of("Marked " + horse.getName().getString()
				+ " as horse A. Look at another horse and use predict or simulate, or /hl foal breed."));
	}

	private static int predict(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse a = HorseCommands.marked(context.getSource());
		AbstractHorse b = HorseCommands.target(context);
		Map<Trait<?>, Distribution<?>> odds = CrossPredictor.predictAll(HorseGenetics.genomeOf(a), HorseGenetics.genomeOf(b), HorseTraits.ALL,
				GeneticsSettings.get().mutationChance());
		List<String> lines = new ArrayList<>();
		lines.add("Foal of " + a.getName().getString() + " and " + b.getName().getString() + ":");
		odds.forEach((trait, distribution) -> lines.add("  " + trait.id() + ": " + percentages(distribution)));
		addSameHorseNote(lines, a, b);
		return HorseCommands.replyLines(context, lines);
	}

	private static int simulate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse a = HorseCommands.marked(context.getSource());
		AbstractHorse b = HorseCommands.target(context);
		int count = IntegerArgumentType.getInteger(context, "count");
		Genome genomeA = HorseGenetics.genomeOf(a);
		Genome genomeB = HorseGenetics.genomeOf(b);
		double mutation = GeneticsSettings.get().mutationChance();
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
		return HorseCommands.replyLines(context, lines);
	}

	// --- Helpers -----------------------------------------------------------------

	private static void addParents(List<String> lines, AbstractHorse horse) {
		Optional<Lineage> lineage = GeneticsView.lineage(horse);
		if (lineage.isEmpty() || !lineage.get().hasParents()) {
			lines.add("Parents: not recorded (older horse)");
		} else {
			lines.add("Parent A" + named(lineage.get().nameA()) + ": " + lineage.get().parentA().map(Genome::toString).orElse("unknown"));
			lines.add("Parent B" + named(lineage.get().nameB()) + ": " + lineage.get().parentB().map(Genome::toString).orElse("unknown"));
		}
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

	/** A vanilla enum's command id, such as "dark_brown". */
	private static String id(Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	private static <E extends Enum<E>> E parse(E[] values, String id) {
		for (E value : values) {
			if (id(value).equals(id)) {
				return value;
			}
		}
		return null;
	}

	private static String names(Enum<?>[] values) {
		return String.join(", ", Arrays.stream(values).map(GeneticsCommands::id).toList());
	}
}
