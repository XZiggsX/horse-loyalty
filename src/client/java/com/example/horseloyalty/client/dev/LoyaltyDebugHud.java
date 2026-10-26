package com.example.horseloyalty.client.dev;

import com.example.horseloyalty.config.HorseLoyaltyConfig;
import com.example.horseloyalty.loyalty.Hunger;
import com.example.horseloyalty.loyalty.Loyalty;
import com.example.horseloyalty.loyalty.LoyaltyData;
import com.example.horseloyalty.client.riding.Gait;
import com.example.horseloyalty.client.riding.GaitHolder;
import com.example.horseloyalty.client.riding.Riding;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Development-only panel in the top left while riding a horse.
 * <p>
 * On by default in the dev environment ({@code gradlew runClient}), off in a built jar.
 * Override with the JVM flag {@code -Dhorseloyalty.devhud=true} or {@code =false}.
 * To remove it completely: delete this {@code dev} package, {@code mixin/client/HudMixin.java}
 * and the {@code "HudMixin"} line in {@code horseloyalty.client.mixins.json}.
 */
public final class LoyaltyDebugHud {
	public static final boolean ENABLED = isEnabled();

	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int HEADER_COLOR = 0xFFFFD27F;
	private static final int BACKGROUND_COLOR = 0x90000000;

	private LoyaltyDebugHud() {
	}

	/** On in the dev environment unless the -Dhorseloyalty.devhud flag says otherwise. */
	private static boolean isEnabled() {
		String flag = System.getProperty("horseloyalty.devhud");
		return flag != null ? Boolean.parseBoolean(flag) : FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	/** Draws the panel. Called from HudMixin every frame; does nothing unless riding a horse. */
	public static void extract(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || !(player.getVehicle() instanceof AbstractHorse horse) || !Loyalty.appliesTo(horse)) {
			return;
		}
		List<String> lines = buildLines(minecraft, player, horse);

		Font font = minecraft.font;
		int x = 4;
		int y = 4;
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, font.width(line));
		}
		graphics.fill(x - 2, y - 2, x + width + 2, y + lines.size() * 10, BACKGROUND_COLOR);
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(font, lines.get(i), x, y + i * 10, i == 0 ? HEADER_COLOR : TEXT_COLOR, true);
		}
	}

	/** The panel text, one string per line. The first line is the header. */
	private static List<String> buildLines(Minecraft minecraft, Player player, AbstractHorse horse) {
		List<String> lines = new ArrayList<>();
		lines.add("Horse Loyalty [dev]");

		// Vanilla doesn't sync a horse's owner to the client, so read it from the server copy.
		AbstractHorse serverHorse = findServerHorse(minecraft, horse);
		String ownerText;
		if (serverHorse == null) {
			ownerText = "unknown";
		} else {
			EntityReference<LivingEntity> owner = serverHorse.getOwnerReference();
			ownerText = owner == null ? "none" : owner.matches(player) ? "you" : "someone else";
		}
		lines.add(horse.getName().getString() + "  tamed: " + (horse.isTamed() ? "yes" : "no") + "  owner: " + ownerText);

		int level = Loyalty.level(horse, player);
		lines.add("Your level: " + level + "/" + Loyalty.MAX_LEVEL + " " + Loyalty.levelName(level).getString());

		// Points, feeds and ride time are server-only data. In singleplayer we can read
		// them straight from the integrated server's copy of the horse.
		if (serverHorse == null) {
			lines.add("(points and stats need singleplayer)");
		} else {
			Map<UUID, LoyaltyData> riders = serverHorse.getAttachedOrElse(Loyalty.DATA, Map.of());
			LoyaltyData data = riders.getOrDefault(player.getUUID(), LoyaltyData.EMPTY);
			int earned = Loyalty.earnedLevel(data);
			String next = earned >= Loyalty.MAX_LEVEL ? "max" : String.valueOf(HorseLoyaltyConfig.get().pointsNeeded(earned + 1));
			lines.add("Points: " + data.points() + "  next level at: " + next);

			int seconds = data.rideTicks() / 20;
			lines.add("Fed: " + data.feeds() + "x  ridden: " + seconds / 60 + "m " + seconds % 60 + "s");

			long sinceFed = serverHorse.level().getGameTime() - data.lastFedTick();
			long cooldownLeft = data.lastFedTick() == 0L ? 0 : Math.max(0, HorseLoyaltyConfig.get().feedCooldownTicks() - sinceFed);
			lines.add("Feed cooldown: " + (cooldownLeft == 0 ? "ready" : cooldownLeft / 20 + "s"));
			lines.add("Riders remembered: " + riders.size() + "/" + Loyalty.MAX_REMEMBERED_RIDERS
					+ "  your points lost to hunger: " + data.lostPoints());

			long secondsSinceAte = Hunger.ticksSinceFed(serverHorse) / 20;
			if (Hunger.isHungry(serverHorse)) {
				lines.add("Hunger: HUNGRY, last ate " + secondsSinceAte + "s ago, next loss in "
						+ Hunger.ticksToNextLoss(serverHorse) / 20 + "s");
			} else {
				long hungryIn = HorseLoyaltyConfig.get().ticksUntilHungry() / 20 - secondsSinceAte;
				lines.add("Hunger: last ate " + secondsSinceAte + "s ago, hungry in " + hungryIn + "s");
			}
		}

		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		lines.add(String.format("Health: %.1f/%.1f  speed attr: %.4f (top ~%.1f b/s)  jump: %.3f",
				horse.getHealth(), horse.getMaxHealth(), speed,
				Gait.GALLOP.speed(speed) * Gait.SPEED_TO_BLOCKS_PER_SECOND,
				horse.getAttributeValue(Attributes.JUMP_STRENGTH)));

		Gait gait = ((GaitHolder) horse).horseloyalty$getGait();
		double top = Gait.GALLOP.speed(speed) * Gait.SPEED_TO_BLOCKS_PER_SECOND;
		double target = Riding.gaitSpeed(gait, speed, horse, player) * Gait.SPEED_TO_BLOCKS_PER_SECOND;
		// Distance actually covered last tick. Not getDeltaMovement(): that is stored after
		// ground friction is applied, so it reads only about 55% of the real speed.
		double actual = Math.hypot(horse.getX() - horse.xo, horse.getZ() - horse.zo) * 20;
		lines.add(String.format("Gait: %d %s  target: %.1f b/s (%.0f%%)",
				gait.ordinal(), gait.displayName().getString(), target, target / top * 100));
		lines.add(String.format("Actual: %.1f b/s (%.0f%% of max)",
				actual, actual / top * 100));
		float turnRate = Riding.turnRate(horse, player);
		lines.add(String.format("Loyalty limits: top speed %.0f%%  turn %.1f deg/tick (radius ~%.1f at target)",
				Riding.topSpeedShare(horse, player) * 100, turnRate,
				target / 20 / Math.toRadians(turnRate)));
		int dropTicks = Riding.gaitDropTicks(horse, player);
		int dropIn = Math.max(0, dropTicks - ((GaitHolder) horse).horseloyalty$getGaitDropTimer());
		lines.add(String.format("Gait drop: %.0f%% chance every %ds without shifting, next roll in %ds",
				Riding.gaitDropChance(horse, player) * 100, dropTicks / 20, dropIn / 20));
		return lines;
	}

	/** The singleplayer server's copy of this horse, which has the server-only data. Null in multiplayer. */
	private static AbstractHorse findServerHorse(Minecraft minecraft, AbstractHorse clientHorse) {
		IntegratedServer server = minecraft.getSingleplayerServer();
		if (server == null) {
			return null;
		}
		ServerLevel level = server.getLevel(clientHorse.level().dimension());
		if (level == null) {
			return null;
		}
		return level.getEntity(clientHorse.getUUID()) instanceof AbstractHorse horse ? horse : null;
	}
}
