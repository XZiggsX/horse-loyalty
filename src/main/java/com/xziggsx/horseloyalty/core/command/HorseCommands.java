package com.xziggsx.horseloyalty.core.command;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The shared {@code /horseloyalty} ({@code /hl}) command tree. Features add a branch through their
 * context; a branch only shows and runs while its feature is on, and a dev branch also needs dev
 * tools. Every command needs operator level 2.
 */
public final class HorseCommands {
	/** The name of the optional horse selector at the end of every command. */
	public static final String HORSE = "horse";
	/** The name of the optional player argument. */
	public static final String PLAYER = "player";

	/** How far away the horse you look at may be. */
	private static final double RANGE = 8;
	private static final String DEV_TOOLS = "devTools";

	private static final SimpleCommandExceptionType NO_HORSE = new SimpleCommandExceptionType(
			Component.translatable("horseloyalty.command.no_horse", (int) RANGE));
	private static final SimpleCommandExceptionType NOT_A_HORSE = new SimpleCommandExceptionType(
			Component.translatable("horseloyalty.command.not_a_horse"));
	private static final SimpleCommandExceptionType NO_MARK = new SimpleCommandExceptionType(
			Component.translatable("horseloyalty.command.no_mark"));

	/** A branch one feature added. */
	private record Branch(String featureId, String name, boolean dev, Consumer<LiteralArgumentBuilder<CommandSourceStack>> build) {
	}

	private static final List<Branch> BRANCHES = new ArrayList<>();
	/** Extra top-level aliases: alias name to the branch it stands for. */
	private static final Map<String, String> ALIASES = new HashMap<>();
	/** The horse each player marked, for breeding and genetics commands. Forgotten on restart. */
	private static final Map<UUID, UUID> MARKED = new HashMap<>();

	private HorseCommands() {
	}

	/** Registers the tree with the game. Called once from the common entrypoint, after the features are wired. */
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	/** Adds a branch, {@code /hl <name> ...}, that only runs while its feature (and for dev, dev tools) is on. Core passes null. */
	public static synchronized void addBranch(String featureId, String name, boolean dev,
			Consumer<LiteralArgumentBuilder<CommandSourceStack>> build) {
		BRANCHES.add(new Branch(featureId, name, dev, build));
	}

	/** Adds a top-level alias for a branch, such as {@code /horsegenes} for {@code /hl genes}. */
	public static synchronized void addAlias(String alias, String branch) {
		ALIASES.put(alias, branch);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("horseloyalty")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
		Map<String, LiteralCommandNode<CommandSourceStack>> branchNodes = new HashMap<>();
		for (Branch branch : BRANCHES) {
			Predicate<CommandSourceStack> on = source -> runs(branch);
			LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(branch.name());
			branch.build().accept(builder);
			LiteralCommandNode<CommandSourceStack> built = builder.build();
			LiteralCommandNode<CommandSourceStack> node = branchNodes.get(branch.name());
			if (node == null) {
				// The branch shows while any of its parts may run; each part checks its own switch.
				// A branch that runs on its own (such as /hl features) keeps its command.
				node = new LiteralCommandNode<>(branch.name(), built.getCommand(), source -> anyOn(branch.name()), null, null, false);
				branchNodes.put(branch.name(), node);
			}
			for (CommandNode<CommandSourceStack> child : built.getChildren()) {
				node.addChild(gated(child, on));
			}
		}
		branchNodes.values().forEach(root::then);
		LiteralCommandNode<CommandSourceStack> rootNode = dispatcher.register(root);
		dispatcher.register(Commands.literal("hl").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(rootNode));
		ALIASES.forEach((alias, branch) -> {
			LiteralCommandNode<CommandSourceStack> target = branchNodes.get(branch);
			if (target != null) {
				dispatcher.register(Commands.literal(alias)
						.requires(source -> Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source) && target.canUse(source))
						.redirect(target));
			}
		});
	}

	private static synchronized boolean anyOn(String name) {
		for (Branch branch : BRANCHES) {
			if (branch.name().equals(name) && runs(branch)) {
				return true;
			}
		}
		return false;
	}

	/** Whether a branch may run now: its feature is on (core's always is), and dev tools too for a dev branch. */
	private static boolean runs(Branch branch) {
		return (branch.featureId() == null || Features.isOn(branch.featureId())) && (!branch.dev() || Features.isOn(DEV_TOOLS));
	}

	/** A copy of a child node that also needs {@code on}. Its own children stay as they are. */
	private static CommandNode<CommandSourceStack> gated(CommandNode<CommandSourceStack> child, Predicate<CommandSourceStack> on) {
		CommandNode<CommandSourceStack> copy = switch (child) {
			case LiteralCommandNode<CommandSourceStack> literal -> new LiteralCommandNode<>(literal.getLiteral(), literal.getCommand(),
					on.and(literal.getRequirement()), literal.getRedirect(), literal.getRedirectModifier(), literal.isFork());
			case ArgumentCommandNode<CommandSourceStack, ?> argument -> copyArgument(argument, on);
			default -> throw new IllegalArgumentException("Unexpected command node " + child.getName());
		};
		for (CommandNode<CommandSourceStack> grandchild : child.getChildren()) {
			copy.addChild(grandchild);
		}
		return copy;
	}

	private static <T> ArgumentCommandNode<CommandSourceStack, T> copyArgument(ArgumentCommandNode<CommandSourceStack, T> argument,
			Predicate<CommandSourceStack> on) {
		return new ArgumentCommandNode<>(argument.getName(), argument.getType(), argument.getCommand(), on.and(argument.getRequirement()),
				argument.getRedirect(), argument.getRedirectModifier(), argument.isFork(), argument.getCustomSuggestions());
	}

	/**
	 * Adds the optional horse selector to a command: {@code node} runs {@code command} on its own
	 * (the horse you look at) and with a selector after it. Returns {@code node}.
	 */
	public static <T extends ArgumentBuilder<CommandSourceStack, T>> T withHorse(T node, Command<CommandSourceStack> command) {
		return node.executes(command).then(Commands.argument(HORSE, EntityArgument.entity()).executes(command));
	}

	/** Adds an optional player, then the optional horse selector, to a command. Returns {@code node}. */
	public static <T extends ArgumentBuilder<CommandSourceStack, T>> T withPlayerAndHorse(T node, Command<CommandSourceStack> command) {
		withHorse(node, command);
		return node.then(withHorse(Commands.argument(PLAYER, EntityArgument.player()), command));
	}

	/** The horse to act on: the selector's if one was given, else the one the player looks at within 8 blocks. */
	public static AbstractHorse target(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		if (has(context, HORSE)) {
			Entity entity = EntityArgument.getEntity(context, HORSE);
			if (entity instanceof AbstractHorse horse && HorseScope.inScope(horse)) {
				return horse;
			}
			throw NOT_A_HORSE.create();
		}
		ServerPlayer player = context.getSource().getPlayerOrException();
		Vec3 eye = player.getEyePosition();
		Vec3 reach = player.getViewVector(1.0F).scale(RANGE);
		AABB area = player.getBoundingBox().expandTowards(reach).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach), area,
				entity -> entity instanceof AbstractHorse horse && horse.isAlive() && HorseScope.inScope(horse), RANGE * RANGE);
		if (hit == null || !(hit.getEntity() instanceof AbstractHorse horse)) {
			throw NO_HORSE.create();
		}
		return horse;
	}

	/** The player argument if given, else whoever ran the command. */
	public static ServerPlayer player(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
		return has(context, argument) ? EntityArgument.getPlayer(context, argument) : context.getSource().getPlayerOrException();
	}

	/** Whether the command was given this argument. */
	public static boolean has(CommandContext<CommandSourceStack> context, String argument) {
		return context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(argument));
	}

	/** Remembers a horse as the player's marked horse, for {@code /hl foal breed} and the genes predictions. */
	public static synchronized void mark(ServerPlayer player, AbstractHorse horse) {
		MARKED.put(player.getUUID(), horse.getUUID());
	}

	/** The player's marked horse, if it's loaded in their dimension. */
	public static synchronized AbstractHorse marked(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		UUID id = MARKED.get(player.getUUID());
		if (id == null || !(source.getLevel().getEntity(id) instanceof AbstractHorse horse) || !horse.isAlive()) {
			throw NO_MARK.create();
		}
		return horse;
	}

	/** Sends lines of text back to whoever ran the command. Returns 1, for a command's result. */
	public static int reply(CommandContext<CommandSourceStack> context, Component message) {
		context.getSource().sendSuccess(() -> message, false);
		return 1;
	}

	/** Sends plain lines back, joined with line breaks. Returns 1. */
	public static int replyLines(CommandContext<CommandSourceStack> context, List<String> lines) {
		return reply(context, Component.literal(String.join("\n", lines)));
	}
}
