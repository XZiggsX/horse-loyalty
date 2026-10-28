package com.xziggsx.horseloyalty.dev.client;

import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.xziggsx.horseloyalty.dev.DevToolsFeature;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * Development panel in the top left, for the horse you ride or look at. Each feature that is on
 * adds its own titled section (see {@link DebugSource}), so this panel knows no feature. Sections
 * stack top to bottom and wrap into a second column when they would run off the screen.
 * <p>
 * Shows only while dev tools are on, and then by default in the dev environment
 * ({@code gradlew runClient}) and not in a built jar. Override with the JVM flag
 * {@code -Dhorseloyalty.devhud=true} or {@code =false}.
 */
public final class DevHud {
	/** Whether the JVM flag or the dev environment allows the panel. Dev tools must be on as well. */
	public static final boolean ALLOWED = isAllowed();

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

	private DevHud() {
	}

	/** On in the dev environment unless the -Dhorseloyalty.devhud flag says otherwise. */
	private static boolean isAllowed() {
		String flag = System.getProperty("horseloyalty.devhud");
		return flag != null ? Boolean.parseBoolean(flag) : FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	/** Whether the panel shows: allowed, and dev tools are on (the server's switch while connected). */
	public static boolean shown() {
		return ALLOWED && Features.isOn(DevToolsFeature.ID);
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
		AbstractHorse horse = player.getVehicle() instanceof AbstractHorse ridden ? ridden
				: minecraft.crosshairPickEntity instanceof AbstractHorse looked ? looked : null;
		if (horse == null || !HorseScope.inScope(horse)) {
			return;
		}
		// Owner, points and hunger are server-only. In singleplayer, read the integrated server's copy.
		draw(graphics, minecraft, DebugSource.sections(horse, findServerHorse(minecraft, horse), player));
	}

	private static int color(DebugTone tone) {
		return switch (tone) {
			case NORMAL -> VALUE_COLOR;
			case DIM -> DIM_COLOR;
			case WARN -> WARN_COLOR;
			case GOOD -> GOOD_COLOR;
		};
	}

	private static int height(DebugSection section) {
		return (1 + section.rows().size()) * LINE_HEIGHT;
	}

	/**
	 * Stacks the sections into columns, starting a new column when the next section would run
	 * into the hotbar, and draws each column on its own background.
	 */
	private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, List<DebugSection> sections) {
		Font font = minecraft.font;
		int maxHeight = Math.max(LINE_HEIGHT * 6, minecraft.getWindow().getGuiScaledHeight() - MARGIN - BOTTOM_RESERVED);
		List<List<DebugSection>> columns = new ArrayList<>();
		List<DebugSection> column = new ArrayList<>();
		int height = 0;
		for (DebugSection section : sections) {
			int needed = height(section) + (column.isEmpty() ? 0 : SECTION_GAP);
			if (!column.isEmpty() && height + needed > maxHeight) {
				columns.add(column);
				column = new ArrayList<>();
				needed = height(section);
				height = 0;
			}
			column.add(section);
			height += needed;
		}
		columns.add(column);

		int x = MARGIN + PADDING;
		for (List<DebugSection> each : columns) {
			x += drawColumn(graphics, font, each, x, MARGIN + PADDING) + PADDING * 2 + PANEL_GAP;
		}
	}

	/** Draws one column of sections with aligned labels and values. Returns its width. */
	private static int drawColumn(GuiGraphicsExtractor graphics, Font font, List<DebugSection> sections, int x, int y) {
		int labelWidth = 0;
		int width = 0;
		int height = 0;
		for (DebugSection section : sections) {
			for (DebugSection.Row row : section.rows()) {
				labelWidth = Math.max(labelWidth, font.width(row.label()));
			}
		}
		for (DebugSection section : sections) {
			width = Math.max(width, font.width(section.title()));
			for (DebugSection.Row row : section.rows()) {
				width = Math.max(width, labelWidth + COLUMN_GAP + font.width(row.value()));
			}
			height += height(section) + (height == 0 ? 0 : SECTION_GAP);
		}
		graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING - 1, BACKGROUND_COLOR);

		int lineY = y;
		for (DebugSection section : sections) {
			graphics.text(font, section.title(), x, lineY, TITLE_COLOR, true);
			lineY += LINE_HEIGHT;
			for (DebugSection.Row row : section.rows()) {
				graphics.text(font, row.label(), x, lineY, LABEL_COLOR, true);
				graphics.text(font, row.value(), x + labelWidth + COLUMN_GAP, lineY, color(row.tone()), true);
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
