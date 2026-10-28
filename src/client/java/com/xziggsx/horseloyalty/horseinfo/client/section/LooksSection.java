package com.xziggsx.horseloyalty.horseinfo.client.section;

import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.TraitLine;
import com.xziggsx.horseloyalty.horseinfo.api.Shown;
import com.xziggsx.horseloyalty.horseinfo.client.GeneNames;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.InfoPalette;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * The real names of what the horse shows, side by side with a small swatch each: "Buckskin",
 * "Tobiano". Horses only; the profile has no looks for donkeys and mules.
 */
public final class LooksSection implements InfoSection {
	private static final int SWATCH = 8;

	@Override
	public int height(HorseProfile profile) {
		return profile.looks() instanceof Shown.Off<List<TraitLine>>
				|| profile.looks() instanceof Shown.Visible<List<TraitLine>>(List<TraitLine> lines) && lines.isEmpty() ? 0 : 22;
	}

	@Override
	public void draw(InfoContext context, HorseProfile profile, Rect area) {
		if (profile.looks() instanceof Shown.Off<List<TraitLine>>) {
			return;
		}
		context.text(Component.translatable("horseloyalty.info.looks"), area.x(), area.y(), InfoPalette.TEXT);
		int y = area.y() + 11;
		switch (profile.looks()) {
			case Shown.Visible<List<TraitLine>>(List<TraitLine> lines) -> {
				String coat = coatOf(lines);
				int columnWidth = lines.isEmpty() ? area.width() : area.width() / lines.size();
				for (int i = 0; i < lines.size(); i++) {
					TraitLine line = lines.get(i);
					int x = area.x() + i * columnWidth;
					drawSwatch(context, line, coat, x, y);
					context.text(GeneNames.traitValue(line.traitId(), line.valueId()), x + SWATCH + 4, y, InfoPalette.TEXT);
				}
			}
			case Shown.Locked<List<TraitLine>>(Component reason) -> context.locked(reason, area.x(), y);
			case Shown.Off<List<TraitLine>> off -> {
			}
		}
	}

	private static String coatOf(List<TraitLine> lines) {
		for (TraitLine line : lines) {
			if (line.traitId().equals("coat")) {
				return line.valueId();
			}
		}
		return "bay";
	}

	/** A coat swatch is its colour; a marking swatch is the coat with the white pattern on it. */
	private static void drawSwatch(InfoContext context, TraitLine line, String coat, int x, int y) {
		context.fill(x, y - 1, SWATCH + 2, SWATCH + 2, InfoPalette.SLOT_SHADOW);
		int inner = x + 1;
		int top = y;
		if (line.traitId().equals("coat")) {
			context.fill(inner, top, SWATCH, SWATCH, InfoPalette.coat(line.valueId()));
			return;
		}
		int base = InfoPalette.coat(coat);
		int white = InfoPalette.WHITE_HAIR;
		switch (line.valueId()) {
			case "tobiano" -> {
				context.fill(inner, top, SWATCH, SWATCH, base);
				context.fill(inner + 3, top, 5, 5, white);
				context.fill(inner, top + 5, 4, 3, white);
			}
			case "fewspot" -> {
				context.fill(inner, top, SWATCH, SWATCH, white);
				dots(context, inner, top, base);
			}
			case "leopard" -> {
				context.fill(inner, top, SWATCH, SWATCH, base);
				dots(context, inner, top, white);
			}
			case "white_socks" -> {
				context.fill(inner, top, SWATCH, SWATCH, base);
				context.fill(inner, top + 5, SWATCH, 3, white);
			}
			default -> context.fill(inner, top, SWATCH, SWATCH, base);
		}
	}

	private static void dots(InfoContext context, int x, int y, int colour) {
		int[][] spots = {{1, 1}, {5, 2}, {2, 5}, {6, 6}};
		for (int[] spot : spots) {
			context.fill(x + spot[0], y + spot[1], 1, 1, colour);
		}
	}
}
