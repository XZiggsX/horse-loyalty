package com.xziggsx.horseloyalty.dev.client;

import com.xziggsx.horseloyalty.config.HorseLoyaltyConfig;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.HorseUnits;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import com.xziggsx.horseloyalty.genetics.core.Locus;
import com.xziggsx.horseloyalty.genetics.view.GeneticsView;
import com.xziggsx.horseloyalty.genetics.view.ShownTrait;
import com.xziggsx.horseloyalty.genetics.world.Lineage;
import com.xziggsx.horseloyalty.hunger.Hunger;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import com.xziggsx.horseloyalty.loyalty.LoyaltyData;
import com.xziggsx.horseloyalty.personality.api.PersonalityView;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.client.GaitHolder;
import com.xziggsx.horseloyalty.riding.client.GaitRaise;
import com.xziggsx.horseloyalty.riding.client.Riding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 * Development-only panel in the top left, for the horse you ride or look at. Split into
 * titled sections (Horse, Loyalty, Riding, Genetics), each only shown when it has something
 * to say: Riding only while riding, Loyalty only for a tamed horse. Sections stack top to
 * bottom and wrap into a second column when they would run off the screen.
 * <p>
 * To add a section, write a method that returns a {@link Section} (or null to hide it) and
 * add it in {@link #sections}.
 * <p>
 * On by default in the dev environment ({@code gradlew runClient}), off in a built jar.
 * Override with the JVM flag {@code -Dhorseloyalty.devhud=true} or {@code =false}.
 * To remove it completely: delete this {@code dev} package, {@code mixin/client/HudMixin.java}
 * and the {@code "HudMixin"} line in {@code horseloyalty.client.mixins.json}.
 */
public final class LoyaltyDebugHud {
	public static final boolean ENABLED = isEnabled();

	private static final int TITLE_COLOR = 0xFFFFD27F;
	private static final int LABEL_COLOR = 0xFFA0A0A0;
	private static final int VALUE_COLOR = 0xFFFFFFFF;
	private static final int DIM_COLOR = 0xFF808080;
	private static final int WARN_COLOR = 0xFFFF7070;
	private static final int GOOD_COLOR = 0xFF8CE68C;
	private static final int BACKGROUND_COLOR = 0x90000000;

	private static final int MARGIN = 4;
	private static final int PADDING = 3;
	private static final int LINE_HEIGHT = 10;
	/** Space between a section's last row and the next title. */
	private static final int SECTION_GAP = 4;
	/** Space between the label and value columns. */
	private static final int COLUMN_GAP = 6;
	/** Space between two panel columns. */
	private static final int PANEL_GAP = 4;
	/** Room left free at the bottom of the screen for the hotbar. */
	private static final int BOTTOM_RESERVED = 48;

	/** One label: value line. */
	private record Row(String label, String value, int color) {
	}

	/** A titled group of rows. */
	private record Section(String title, List<Row> rows) {
		Section(String title) {
			this(title, new ArrayList<>());
		}

		Section add(String label, String value) {
			return add(label, value, VALUE_COLOR);
		}

		Section add(String label, String value, int color) {
			rows.add(new Row(label, value, color));
			return this;
		}

		int height() {
			return (1 + rows.size()) * LINE_HEIGHT;
		}
	}

	private LoyaltyDebugHud() {
	}

	/** On in the dev environment unless the -Dhorseloyalty.devhud flag says otherwise. */
	private static boolean isEnabled() {
		String flag = System.getProperty("horseloyalty.devhud");
		return flag != null ? Boolean.parseBoolean(flag) : FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	/**
	 * Draws the panel. Called from HudMixin every frame. Shows the horse you ride, or else the
	 * one you look at; draws nothing otherwise.
	 */
	public static void extract(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null) {
			return;
		}
		boolean riding = player.getVehicle() instanceof AbstractHorse;
		AbstractHorse horse = riding ? (AbstractHorse) player.getVehicle()
				: minecraft.crosshairPickEntity instanceof AbstractHorse looked ? looked : null;
		if (horse == null || !HorseScope.inScope(horse)) {
			return;
		}
		draw(graphics, minecraft, sections(minecraft, player, horse, riding));
	}

	// --- Sections ----------------------------------------------------------------

	/** Every section to show, top to bottom. Hidden sections are left out. */
	private static List<Section> sections(Minecraft minecraft, Player player, AbstractHorse horse, boolean riding) {
		// Owner, points and hunger are server-only. In singleplayer, read the integrated server's copy.
		AbstractHorse serverHorse = findServerHorse(minecraft, horse);
		List<Section> sections = new ArrayList<>();
		sections.add(horseSection(player, horse, serverHorse, riding));
		addIfShown(sections, loyaltySection(player, horse, serverHorse));
		if (riding) {
			sections.add(ridingSection(player, horse));
		}
		sections.add(geneticsSection(horse));
		return sections;
	}

	private static void addIfShown(List<Section> sections, Section section) {
		if (section != null) {
			sections.add(section);
		}
	}

	/** Who the horse is: name, kind, owner, personality and its stats. */
	private static Section horseSection(Player player, AbstractHorse horse, AbstractHorse serverHorse, boolean riding) {
		Section section = new Section("Horse  [dev, " + (riding ? "riding" : "looking") + "]");
		section.add("Name", horse.getName().getString() + "  (" + horse.getType().getDescription().getString() + ")");
		section.add("Tamed", horse.isTamed() ? "yes, owner " + ownerText(player, serverHorse) : "no",
				horse.isTamed() ? VALUE_COLOR : DIM_COLOR);
		section.add("Personality", PersonalityView.get().of(horse).displayName().getString()
				+ (PersonalityView.get().revealedTo(horse, player).isPresent() ? "" : "  (hidden from you)"));
		section.add("Health", String.format(Locale.ROOT, "%.1f / %.1f", horse.getHealth(), horse.getMaxHealth()));
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		section.add("Speed", String.format(Locale.ROOT, "%.4f  (gallop ~%.1f b/s)",
				speed, Riding.baseSpeed(Gait.GALLOP, speed) * HorseUnits.SPEED_TO_BLOCKS_PER_SECOND));
		section.add("Jump", String.format(Locale.ROOT, "%.3f", horse.getAttributeValue(Attributes.JUMP_STRENGTH)));
		return section;
	}

	/** Your loyalty with the horse and its hunger. Hidden for an untamed horse. */
	private static Section loyaltySection(Player player, AbstractHorse horse, AbstractHorse serverHorse) {
		if (!horse.isTamed()) {
			return null;
		}
		HorseLoyaltyConfig config = HorseLoyaltyConfig.get();
		Section section = new Section("Loyalty");
		int level = Loyalty.level(horse, player);
		section.add("Level", level + "/" + Loyalty.MAX_LEVEL + "  " + Loyalty.levelName(level).getString());
		if (serverHorse == null) {
			section.add("Stats", "singleplayer only", DIM_COLOR);
			return section;
		}

		Map<UUID, LoyaltyData> riders = serverHorse.getAttachedOrElse(Loyalty.DATA, Map.of());
		LoyaltyData data = riders.getOrDefault(player.getUUID(), LoyaltyData.EMPTY);
		int earned = Loyalty.earnedLevel(data);
		section.add("Points", data.points() + (earned >= Loyalty.MAX_LEVEL ? "  (max)"
				: "  (next at " + config.pointsNeeded(earned + 1) + ")"));
		if (data.lostPoints() > 0) {
			section.add("Lost", data.lostPoints() + " to hunger", WARN_COLOR);
		}

		long sinceFed = serverHorse.level().getGameTime() - data.lastFedTick();
		long cooldown = data.lastFedTick() == 0L ? 0 : Math.max(0, config.feedCooldownTicks() - sinceFed);
		section.add("Fed", data.feeds() + "x, " + (cooldown == 0 ? "treat ready" : "next treat in " + seconds(cooldown)),
				cooldown == 0 ? GOOD_COLOR : VALUE_COLOR);
		section.add("Ridden", seconds(data.rideTicks()));
		section.add("Riders", riders.size() + "/" + Loyalty.MAX_REMEMBERED_RIDERS + " remembered");

		long sinceAte = Hunger.ticksSinceFed(serverHorse);
		if (Hunger.isHungry(serverHorse)) {
			section.add("Hunger", "HUNGRY, loses points in " + seconds(Hunger.ticksToNextLoss(serverHorse)), WARN_COLOR);
		} else {
			section.add("Hunger", "fed " + seconds(sinceAte) + " ago, hungry in "
					+ seconds(Math.max(0, config.ticksUntilHungry() - sinceAte)));
		}
		return section;
	}

	/** Gait, speed against target, loyalty limits, gait drops and balking. Only while riding. */
	private static Section ridingSection(Player player, AbstractHorse horse) {
		Section section = new Section("Riding");
		GaitHolder holder = (GaitHolder) horse;
		Gait gait = holder.horseloyalty$getGait();
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double top = Riding.baseSpeed(Gait.GALLOP, speed) * HorseUnits.SPEED_TO_BLOCKS_PER_SECOND;
		double push = holder.horseloyalty$getGallopPush().extra();
		double target = Riding.gaitSpeed(gait, speed, horse, player) * HorseUnits.SPEED_TO_BLOCKS_PER_SECOND * (1 + push);
		// Distance actually covered last tick. Not getDeltaMovement(): that is stored after
		// ground friction is applied, so it reads only about 55% of the real speed.
		double actual = Math.hypot(horse.getX() - horse.xo, horse.getZ() - horse.zo) * 20;

		section.add("Gait", gait.ordinal() + " " + gait.displayName().getString()
				+ (push > 0 ? String.format(Locale.ROOT, "  push +%.0f%%", push * 100) : ""));
		section.add("Speed", String.format(Locale.ROOT, "%.1f b/s  (target %.1f, %.0f%% of max)",
				actual, target, target / top * 100));
		float turnRate = Riding.turnRate(horse, player);
		section.add("Limits", String.format(Locale.ROOT, "top %.0f%%, turn %.1f deg/t",
				Riding.topSpeedShare(horse, player) * 100, turnRate));

		int dropTicks = Riding.gaitDropTicks(horse, player);
		int dropIn = Math.max(0, dropTicks - holder.horseloyalty$getGaitDropTimer());
		section.add("Gait drop", String.format(Locale.ROOT, "%.0f%% every %s, next roll in %s",
				Riding.gaitDropChance(horse, player) * 100, seconds(dropTicks), seconds(dropIn)));

		int raiseCooldown = holder.horseloyalty$getRaiseCooldown();
		double balk = GaitRaise.failChance(horse, player, gait) * 100;
		if (raiseCooldown > 0) {
			section.add("Balk", "unsettled for " + seconds(raiseCooldown + 19), WARN_COLOR);
		} else {
			section.add("Balk", String.format(Locale.ROOT, "%.1f%% on shifting up", balk), balk > 0 ? VALUE_COLOR : DIM_COLOR);
		}
		return section;
	}

	/**
	 * What the horse shows, its genes one locus per row, and whether its parents are known.
	 * Genes and lineage are synced, so this works in multiplayer too.
	 */
	private static Section geneticsSection(AbstractHorse horse) {
		Section section = new Section("Genetics");
		for (ShownTrait<?> shown : GeneticsView.shownTraits(horse)) {
			String id = shown.trait().id();
			section.add(Character.toUpperCase(id.charAt(0)) + id.substring(1),
					shown.current() instanceof Enum<?> value ? Locus.name(value) : "none");
		}

		Optional<Genome> genome = GeneticsView.genome(horse);
		if (genome.isEmpty()) {
			section.add("Genes", HorseLoyaltyConfig.get().geneticsEnabled() ? "none yet" : "off in config", DIM_COLOR);
		} else {
			genome.get().pairs().forEach((locus, pair) -> section.add(locus.id(), geneText(pair.first(), pair.second()), DIM_COLOR));
		}

		Optional<Lineage> lineage = GeneticsView.lineage(horse);
		if (lineage.isEmpty() || !lineage.get().hasParents()) {
			section.add("Parents", "not recorded (older horse)", DIM_COLOR);
		} else {
			Lineage parents = lineage.get();
			boolean anyNamed = parents.nameA().isPresent() || parents.nameB().isPresent();
			String names = parents.nameA().orElse("unnamed") + " x " + parents.nameB().orElse("unnamed");
			section.add("Parents", "known" + (anyNamed ? "  (" + names + ")" : ""));
		}
		return section;
	}

	// --- Formatting and drawing --------------------------------------------------

	/** Whether vanilla's owner (not synced to clients) is you, from the server copy. */
	private static String ownerText(Player player, AbstractHorse serverHorse) {
		if (serverHorse == null) {
			return "unknown";
		}
		EntityReference<LivingEntity> owner = serverHorse.getOwnerReference();
		return owner == null ? "none" : owner.matches(player) ? "you" : "someone else";
	}

	/** A gene pair, such as "seal_brown / bay". */
	private static String geneText(Enum<?> first, Enum<?> second) {
		return Locus.name(first) + " / " + Locus.name(second);
	}

	/** Ticks as "45s" or "2m 05s". */
	private static String seconds(long ticks) {
		long seconds = ticks / 20;
		return seconds < 60 ? seconds + "s" : String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
	}

	/**
	 * Stacks the sections into columns, starting a new column when the next section would run
	 * into the hotbar, and draws each column on its own background.
	 */
	private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, List<Section> sections) {
		Font font = minecraft.font;
		int maxHeight = Math.max(LINE_HEIGHT * 6, minecraft.getWindow().getGuiScaledHeight() - MARGIN - BOTTOM_RESERVED);
		List<List<Section>> columns = new ArrayList<>();
		List<Section> column = new ArrayList<>();
		int height = 0;
		for (Section section : sections) {
			int needed = section.height() + (column.isEmpty() ? 0 : SECTION_GAP);
			if (!column.isEmpty() && height + needed > maxHeight) {
				columns.add(column);
				column = new ArrayList<>();
				needed = section.height();
				height = 0;
			}
			column.add(section);
			height += needed;
		}
		columns.add(column);

		int x = MARGIN + PADDING;
		for (List<Section> each : columns) {
			x += drawColumn(graphics, font, each, x, MARGIN + PADDING) + PADDING * 2 + PANEL_GAP;
		}
	}

	/** Draws one column of sections with aligned labels and values. Returns its width. */
	private static int drawColumn(GuiGraphicsExtractor graphics, Font font, List<Section> sections, int x, int y) {
		int labelWidth = 0;
		int width = 0;
		int height = 0;
		for (Section section : sections) {
			for (Row row : section.rows()) {
				labelWidth = Math.max(labelWidth, font.width(row.label()));
			}
		}
		for (Section section : sections) {
			width = Math.max(width, font.width(section.title()));
			for (Row row : section.rows()) {
				width = Math.max(width, labelWidth + COLUMN_GAP + font.width(row.value()));
			}
			height += section.height() + (height == 0 ? 0 : SECTION_GAP);
		}
		graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING - 1, BACKGROUND_COLOR);

		int lineY = y;
		for (Section section : sections) {
			graphics.text(font, section.title(), x, lineY, TITLE_COLOR, true);
			lineY += LINE_HEIGHT;
			for (Row row : section.rows()) {
				graphics.text(font, row.label(), x, lineY, LABEL_COLOR, true);
				graphics.text(font, row.value(), x + labelWidth + COLUMN_GAP, lineY, row.color(), true);
				lineY += LINE_HEIGHT;
			}
			lineY += SECTION_GAP;
		}
		return width;
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
