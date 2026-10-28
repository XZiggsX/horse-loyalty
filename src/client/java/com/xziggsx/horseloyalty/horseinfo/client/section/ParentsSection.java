package com.xziggsx.horseloyalty.horseinfo.client.section;

import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.Parents;
import com.xziggsx.horseloyalty.horseinfo.api.Shown;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.InfoPalette;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/** The parents' names: "Thunder × unnamed", or "not recorded (older horse)". */
public final class ParentsSection implements InfoSection {

	@Override
	public int height(HorseProfile profile) {
		return profile.parents() instanceof Shown.Off<Parents> ? 0 : 10;
	}

	@Override
	public void draw(InfoContext context, HorseProfile profile, Rect area) {
		if (profile.parents() instanceof Shown.Off<Parents>) {
			return;
		}
		context.text(Component.translatable("horseloyalty.info.parents"), area.x(), area.y(), InfoPalette.DIM);
		int x = area.x() + InfoLayout.LABEL_WIDTH;
		switch (profile.parents()) {
			case Shown.Visible<Parents>(Parents parents) when parents.recorded() -> context.text(
					Component.translatable("horseloyalty.info.parents.names", name(parents.nameA()), name(parents.nameB())),
					x, area.y(), InfoPalette.TEXT);
			case Shown.Visible<Parents> unused -> context.text(Component.translatable("horseloyalty.info.parents.not_recorded"),
					x, area.y(), InfoPalette.DIM);
			case Shown.Locked<Parents>(Component reason) -> context.locked(reason, x, area.y());
			case Shown.Off<Parents> off -> {
			}
		}
	}

	private static Component name(Optional<String> name) {
		return name.<Component>map(Component::literal).orElse(Component.translatable("horseloyalty.info.parents.unnamed"));
	}
}
