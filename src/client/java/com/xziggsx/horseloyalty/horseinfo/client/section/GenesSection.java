package com.xziggsx.horseloyalty.horseinfo.client.section;

import com.xziggsx.horseloyalty.horseinfo.HorseProfile.GeneRow;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.Shown;
import com.xziggsx.horseloyalty.horseinfo.client.GeneNames;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.InfoPalette;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * The gene table: a row per gene with both copies, one from each parent, written out in full.
 * Copies that decide the look are coloured, copies that are only carried are grey. Hovering a
 * copy says which it is. Scrolls (mouse wheel) when it doesn't fit, with a small scrollbar.
 */
public final class GenesSection implements InfoSection {
	static final int TITLE_HEIGHT = 11;
	static final int ROW_HEIGHT = 11;
	private static final int CHIP_HEIGHT = 10;
	private static final int CHIP_GAP = 3;
	private static final int SCROLLBAR = 4;

	@Override
	public int height(HorseProfile profile) {
		return TITLE_HEIGHT + rowCount(profile) * ROW_HEIGHT;
	}

	private static int rowCount(HorseProfile profile) {
		return profile.genes() instanceof Shown.Visible<List<GeneRow>>(List<GeneRow> rows) ? rows.size() : 1;
	}

	/** The scrolling part of the table: everything below the title. */
	public static Rect rowsArea(Rect area) {
		return new Rect(area.x(), area.y() + TITLE_HEIGHT, area.width(), Math.max(0, area.height() - TITLE_HEIGHT));
	}

	/** How far the table can scroll in this box, 0 if it fits. */
	public static int maxScroll(HorseProfile profile, Rect area) {
		return Math.max(0, rowCount(profile) * ROW_HEIGHT - rowsArea(area).height());
	}

	@Override
	public void draw(InfoContext context, HorseProfile profile, Rect area) {
		context.text(Component.translatable("horseloyalty.info.genes"), area.x(), area.y(), InfoPalette.TEXT);
		Component hint = Component.translatable("horseloyalty.info.genes.hint");
		context.text(hint, area.right() - context.font().width(hint), area.y(), InfoPalette.DIM);
		Rect rows = rowsArea(area);
		switch (profile.genes()) {
			case Shown.Visible<List<GeneRow>>(List<GeneRow> genes) -> drawRows(context, genes, rows, maxScroll(profile, area));
			case Shown.Locked<List<GeneRow>>(Component reason) -> context.locked(reason, rows.x(), rows.y());
		}
	}

	private static void drawRows(InfoContext context, List<GeneRow> genes, Rect rows, int maxScroll) {
		int width = rows.width() - (maxScroll > 0 ? SCROLLBAR + 2 : 0);
		int nameWidth = 0;
		for (GeneRow row : genes) {
			nameWidth = Math.max(nameWidth, context.font().width(GeneNames.locus(row.locusId())));
		}
		nameWidth = Math.min(nameWidth + 6, width * 2 / 5);
		int chipWidth = (width - nameWidth - CHIP_GAP) / 2;

		context.graphics().enableScissor(context.left() + rows.x(), context.top() + rows.y(),
				context.left() + rows.right(), context.top() + rows.bottom());
		int y = rows.y() - context.scroll();
		for (GeneRow row : genes) {
			if (y + ROW_HEIGHT > rows.y() && y < rows.bottom()) {
				context.text(GeneNames.locus(row.locusId()), rows.x(), y + 1, InfoPalette.DIM);
				int chipX = rows.x() + nameWidth;
				drawChip(context, rows, row, row.firstId(), row.role().firstShows(), chipX, y, chipWidth);
				drawChip(context, rows, row, row.secondId(), row.role().secondShows(), chipX + chipWidth + CHIP_GAP, y, chipWidth);
			}
			y += ROW_HEIGHT;
		}
		context.graphics().disableScissor();
		if (maxScroll > 0) {
			drawScrollbar(context, rows, genes.size() * ROW_HEIGHT, maxScroll);
		}
	}

	private static void drawChip(InfoContext context, Rect rows, GeneRow row, String versionId, boolean shows,
			int x, int y, int width) {
		Component name = GeneNames.version(row.locusId(), versionId);
		context.fill(x, y, width, CHIP_HEIGHT, InfoPalette.SLOT_SHADOW);
		context.fill(x + 1, y + 1, width - 2, CHIP_HEIGHT - 2, shows ? InfoPalette.chip(row.locusId(), versionId) : InfoPalette.CHIP_CARRIED);
		String text = context.font().plainSubstrByWidth(name.getString(), width - 4);
		context.text(Component.literal(text), x + 2, y + 1, shows ? InfoPalette.CHIP_TEXT : InfoPalette.CHIP_CARRIED_TEXT);
		Rect chip = new Rect(x, y, width, CHIP_HEIGHT);
		if (context.hovering(chip) && context.hovering(rows)) {
			context.graphics().setTooltipForNextFrame(Component.translatable(
					shows ? "horseloyalty.info.copy.shows" : "horseloyalty.info.copy.carried",
					GeneNames.locus(row.locusId()), name), context.mouseX(), context.mouseY());
		}
	}

	private static void drawScrollbar(InfoContext context, Rect rows, int contentHeight, int maxScroll) {
		int x = rows.right() - SCROLLBAR;
		context.fill(x, rows.y(), SCROLLBAR, rows.height(), InfoPalette.SCROLL_TRACK);
		int thumbHeight = Math.max(6, rows.height() * rows.height() / contentHeight);
		int thumbY = rows.y() + (rows.height() - thumbHeight) * context.scroll() / maxScroll;
		context.fill(x, thumbY, SCROLLBAR, thumbHeight, InfoPalette.SCROLL_THUMB);
	}
}
