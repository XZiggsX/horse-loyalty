package com.xziggsx.horseloyalty.horseinfo.client.section;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.Identity;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile.LoyaltyLine;
import com.xziggsx.horseloyalty.horseinfo.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.Shown;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.InfoPalette;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** The horse's kind and personality, then your loyalty as horseshoes with its level name. */
public final class HeaderSection implements InfoSection {
	private static final Identifier SHOE_FULL = Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "hud/loyalty_full");
	private static final Identifier SHOE_EMPTY = Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "hud/loyalty_empty");
	private static final int SHOE_SIZE = 9;
	private static final int SHOE_STEP = 10;

	@Override
	public int height(HorseProfile profile) {
		return 21;
	}

	@Override
	public void draw(InfoContext context, HorseProfile profile, Rect area) {
		switch (profile.identity()) {
			case Shown.Visible<Identity>(Identity identity) -> context.text(Component.translatable("horseloyalty.info.identity",
					identity.kind(), identity.personality().displayName()), area.x(), area.y(), InfoPalette.TEXT);
			case Shown.Locked<Identity>(Component reason) -> context.locked(reason, area.x(), area.y());
		}
		int y = area.y() + 11;
		switch (profile.loyalty()) {
			case Shown.Visible<LoyaltyLine>(LoyaltyLine loyalty) -> {
				for (int i = 0; i < loyalty.max(); i++) {
					context.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, i < loyalty.level() ? SHOE_FULL : SHOE_EMPTY,
							context.left() + area.x() + i * SHOE_STEP, context.top() + y - 1, SHOE_SIZE, SHOE_SIZE);
				}
				context.text(Component.translatable("horseloyalty.info.loyalty", loyalty.level(), loyalty.max(), loyalty.name()),
						area.x() + loyalty.max() * SHOE_STEP + 3, y, InfoPalette.TEXT);
			}
			case Shown.Locked<LoyaltyLine>(Component reason) -> context.locked(reason, area.x(), y);
		}
	}
}
