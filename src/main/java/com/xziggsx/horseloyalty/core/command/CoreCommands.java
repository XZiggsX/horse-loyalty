package com.xziggsx.horseloyalty.core.command;

import com.xziggsx.horseloyalty.core.config.ClientPrefs;
import com.xziggsx.horseloyalty.core.config.ConfigFile;
import com.xziggsx.horseloyalty.core.config.SettingsSync;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

/**
 * The mod's own commands: {@code /hl features} lists the switches, {@code /hl reload} reads the
 * config again and {@code /hl info} shows everything every feature knows about a horse.
 */
public final class CoreCommands {
	private CoreCommands() {
	}

	/** Adds the core branches. Called once from the common entrypoint. */
	public static void init() {
		HorseCommands.addBranch(null, "features", false, branch -> branch.executes(CoreCommands::features));
		HorseCommands.addBranch(null, "reload", false, branch -> branch.executes(CoreCommands::reload));
		HorseCommands.addBranch(null, "info", false, branch -> HorseCommands.withHorse(branch, CoreCommands::info));
	}

	/** Every feature, whether it's on, and why if a requirement keeps it off. */
	private static int features(CommandContext<CommandSourceStack> context) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("horseloyalty.command.features.title"));
		for (Feature feature : Features.all()) {
			String id = feature.id();
			Optional<String> because = Features.offBecause(id);
			Component state = Features.isOn(id) ? Component.translatable("horseloyalty.command.features.on")
					: because.isPresent() ? Component.translatable("horseloyalty.command.features.needs", because.get())
					: Component.translatable("horseloyalty.command.features.off");
			lines.add(Component.translatable("horseloyalty.command.features.line", id, state));
		}
		return HorseCommands.reply(context, join(lines));
	}

	/** Reads the config again (global file plus the world's override) and syncs every player. */
	private static int reload(CommandContext<CommandSourceStack> context) {
		ConfigFile.load();
		ClientPrefs.load();
		SettingsSync.sendToAll(context.getSource().getServer());
		return HorseCommands.reply(context, Component.translatable("horseloyalty.command.reload"));
	}

	/** One block per feature about the horse, from the same sources as the dev HUD. */
	private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		AbstractHorse horse = HorseCommands.target(context);
		ServerPlayer player = context.getSource().getPlayer();
		List<String> lines = new ArrayList<>();
		for (DebugSection section : DebugSource.sections(horse, horse, player)) {
			lines.addAll(section.lines());
		}
		return HorseCommands.replyLines(context, lines);
	}

	/** Lines of components joined with line breaks. */
	public static Component join(List<Component> lines) {
		MutableComponent result = Component.empty();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) {
				result.append("\n");
			}
			result.append(lines.get(i));
		}
		return result;
	}
}
