package com.xziggsx.horseloyalty.horseinfo.client.section;

import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.StatLine;
import com.xziggsx.horseloyalty.horseinfo.api.Shown;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.InfoPalette;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** A bar per stat line, with its name on the left and its value on the right. Driven by the profile's list. */
public final class StatsSection implements InfoSection {
	private static final int ROW_HEIGHT = 10;
	private static final int VALUE_WIDTH = 52;
	private static final int BAR_HEIGHT = 5;

	@Override
	public int height(HorseProfile profile) {
		return profile.stats() instanceof Shown.Visible<List<StatLine>>(List<StatLine> lines) ? lines.size() * ROW_HEIGHT + 2 : ROW_HEIGHT;
	}

	@Override
	public void draw(InfoContext context, HorseProfile profile, Rect area) {
		switch (profile.stats()) {
			case Shown.Visible<List<StatLine>>(List<StatLine> lines) -> {
				int y = area.y();
				for (StatLine line : lines) {
					drawLine(context, line, area.x(), y, area.width());
					y += ROW_HEIGHT;
				}
			}
			case Shown.Locked<List<StatLine>>(Component reason) -> context.locked(reason, area.x(), area.y());
			case Shown.Off<List<StatLine>> off -> {
			}
		}
	}

	private static void drawLine(InfoContext context, StatLine line, int x, int y, int width) {
		context.text(Component.translatable("horseloyalty.info.stat." + line.id()), x, y, InfoPalette.DIM);
		int barX = x + InfoLayout.LABEL_WIDTH;
		int barWidth = Math.max(10, width - InfoLayout.LABEL_WIDTH - VALUE_WIDTH - 4);
		context.fill(barX, y + 1, barWidth, BAR_HEIGHT + 2, InfoPalette.SLOT_SHADOW);
		context.fill(barX + 1, y + 2, barWidth - 2, BAR_HEIGHT, InfoPalette.BAR_BACK);
		context.fill(barX + 1, y + 2, (int) Math.round((barWidth - 2) * line.fill()), BAR_HEIGHT, InfoPalette.bar(line.id()));
		context.text(Component.literal(value(line)), barX + barWidth + 4, y, InfoPalette.TEXT);
	}

	/** The number next to a bar, by stat id. */
	private static String value(StatLine line) {
		return switch (line.id()) {
			case "health" -> String.format(Locale.ROOT, "%.0f/%.0f", line.value(), line.max());
			case "speed" -> String.format(Locale.ROOT, "%.1f b/s", line.value());
			case "jump" -> String.format(Locale.ROOT, "%.1f blocks", line.value());
			default -> String.format(Locale.ROOT, "%.1f", line.value());
		};
	}
}
