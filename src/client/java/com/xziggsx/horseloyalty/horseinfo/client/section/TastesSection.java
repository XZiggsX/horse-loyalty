package com.xziggsx.horseloyalty.horseinfo.client.section;

import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.TastesLine;
import com.xziggsx.horseloyalty.horseinfo.api.Shown;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.InfoPalette;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.chat.Component;

/** The foods the horse is known to love and dislike: "♥ Apple, Sugar, ?  ✗ ?". Cut short if it doesn't fit. */
public final class TastesSection implements InfoSection {

	@Override
	public int height(HorseProfile profile) {
		return profile.tastes() instanceof Shown.Off<TastesLine> ? 0 : 10;
	}

	@Override
	public void draw(InfoContext context, HorseProfile profile, Rect area) {
		if (profile.tastes() instanceof Shown.Off<TastesLine>) {
			return;
		}
		context.text(Component.translatable("horseloyalty.info.tastes"), area.x(), area.y(), InfoPalette.DIM);
		int x = area.x() + InfoLayout.LABEL_WIDTH;
		switch (profile.tastes()) {
			case Shown.Visible<TastesLine>(TastesLine tastes) -> {
				String text = Component.translatable("horseloyalty.info.tastes.loves", foods(tastes.loves(), tastes.hiddenLoves())).getString()
						+ "  " + Component.translatable("horseloyalty.info.tastes.dislikes", foods(tastes.dislikes(), tastes.hiddenDislikes())).getString();
				String fitted = context.font().plainSubstrByWidth(text, area.right() - x);
				context.text(Component.literal(fitted), x, area.y(), InfoPalette.TEXT);
			}
			case Shown.Locked<TastesLine>(Component reason) -> context.locked(reason, x, area.y());
			case Shown.Off<TastesLine> off -> {
			}
		}
	}

	/** The known foods' names, then a "?" per unknown one, or "none". */
	private static String foods(List<Component> known, int hidden) {
		List<String> parts = new ArrayList<>();
		known.forEach(name -> parts.add(name.getString()));
		parts.addAll(Collections.nCopies(hidden, Component.translatable("horseloyalty.info.tastes.unknown").getString()));
		return parts.isEmpty() ? Component.translatable("horseloyalty.info.tastes.none").getString() : String.join(", ", parts);
	}
}
