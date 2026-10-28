package com.xziggsx.horseloyalty.client.horseinfo.section;

import com.xziggsx.horseloyalty.client.horseinfo.InfoLayout.Rect;
import com.xziggsx.horseloyalty.client.horseinfo.InfoPalette;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.Parents;
import com.xziggsx.horseloyalty.horseinfo.Shown;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/** The parents' names: "Thunder × unnamed", or "not recorded (older horse)". */
public final class ParentsSection implements InfoSection {
	private static final int LABEL_WIDTH = 42;

	@Override
	public int height(HorseProfile profile) {
		return 10;
	}

	@Override
	public void draw(InfoContext context, HorseProfile profile, Rect area) {
		context.text(Component.translatable("horseloyalty.info.parents"), area.x(), area.y(), InfoPalette.DIM);
		int x = area.x() + LABEL_WIDTH;
		switch (profile.parents()) {
			case Shown.Visible<Parents>(Parents parents) when parents.recorded() -> context.text(
					Component.translatable("horseloyalty.info.parents.names", name(parents.nameA()), name(parents.nameB())),
					x, area.y(), InfoPalette.TEXT);
			case Shown.Visible<Parents> unused -> context.text(Component.translatable("horseloyalty.info.parents.not_recorded"),
					x, area.y(), InfoPalette.DIM);
			case Shown.Locked<Parents>(Component reason) -> context.locked(reason, x, area.y());
		}
	}

	private static Component name(Optional<String> name) {
		return name.<Component>map(Component::literal).orElse(Component.translatable("horseloyalty.info.parents.unnamed"));
	}
}
