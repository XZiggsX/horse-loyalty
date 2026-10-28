package com.xziggsx.horseloyalty.horseinfo.client;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Layout;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.section.GenesSection;
import com.xziggsx.horseloyalty.horseinfo.client.section.HeaderSection;
import com.xziggsx.horseloyalty.horseinfo.client.section.InfoContext;
import com.xziggsx.horseloyalty.horseinfo.client.section.InfoSection;
import com.xziggsx.horseloyalty.horseinfo.client.section.LooksSection;
import com.xziggsx.horseloyalty.horseinfo.client.section.ParentsSection;
import com.xziggsx.horseloyalty.horseinfo.client.section.StatsSection;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws the info view: its own background, the title, the horse in the top left, and the
 * sections where {@link InfoLayout} puts them. Which sections go beside the model is the
 * {@link #SIDE} list; the looks and genes have their own boxes below.
 */
public final class HorseInfoPanel {
	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "horse_info/background");
	private static final int MODEL_SCALE = 17;

	/** Beside the horse, top to bottom. */
	private static final List<InfoSection> SIDE = List.of(new HeaderSection(), new StatsSection(), new ParentsSection());
	private static final LooksSection LOOKS = new LooksSection();
	private static final GenesSection GENES = new GenesSection();

	private HorseInfoPanel() {
	}

	/** The layout for this profile in a window of this size. */
	public static Layout layout(HorseProfile profile, int width, int height) {
		return InfoLayout.compute(width, height, LOOKS.height(profile) > 0);
	}

	/** The window height that fits everything without scrolling, so a donkey's short view isn't mostly empty. */
	public static int neededHeight(HorseProfile profile, int width) {
		Layout layout = InfoLayout.compute(width, InfoLayout.PREFERRED_HEIGHT, LOOKS.height(profile) > 0);
		return layout.genes().y() + GENES.height(profile) + InfoLayout.PADDING;
	}

	/** How far the gene table can scroll. */
	public static int maxScroll(HorseProfile profile, int width, int height) {
		return GenesSection.maxScroll(profile, layout(profile, width, height).genes());
	}

	/** The part of the window (relative to it) the mouse wheel scrolls. */
	public static Rect scrollArea(HorseProfile profile, int width, int height) {
		return layout(profile, width, height).genes();
	}

	/**
	 * Draws the whole view. Called every frame from the horse screen's background pass; only
	 * reads the profile.
	 *
	 * @param left   the window's left edge on screen
	 * @param top    the window's top edge on screen
	 * @param scroll how far the gene table is scrolled
	 */
	public static void draw(GuiGraphicsExtractor graphics, Font font, LivingEntity horse, HorseProfile profile,
			Component title, int left, int top, int width, int height, int mouseX, int mouseY, int scroll) {
		Layout layout = layout(profile, width, height);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, width, height);
		InfoContext context = new InfoContext(graphics, font, left, top, mouseX, mouseY, scroll);
		context.text(title, InfoLayout.PADDING + 1, 6, InfoPalette.TEXT);

		drawModel(graphics, context, layout.model(), horse, mouseX, mouseY);

		int y = layout.side().y();
		for (InfoSection section : SIDE) {
			int sectionHeight = section.height(profile);
			section.draw(context, profile, new Rect(layout.side().x(), y, layout.side().width(), sectionHeight));
			y += sectionHeight;
		}

		context.fill(InfoLayout.PADDING, layout.separatorY(), width - 2 * InfoLayout.PADDING, 1, InfoPalette.DIM);
		context.fill(InfoLayout.PADDING, layout.separatorY() + 1, width - 2 * InfoLayout.PADDING, 1, InfoPalette.SLOT_LIGHT);
		if (layout.looks().height() > 0) {
			LOOKS.draw(context, profile, layout.looks());
		}
		GENES.draw(context, profile, layout.genes());
	}

	/** The horse in a sunken black box, turning to follow the mouse like vanilla's. */
	private static void drawModel(GuiGraphicsExtractor graphics, InfoContext context, Rect box, LivingEntity horse,
			int mouseX, int mouseY) {
		context.fill(box.x(), box.y(), box.width(), box.height(), InfoPalette.SLOT_LIGHT);
		context.fill(box.x(), box.y(), box.width() - 1, box.height() - 1, InfoPalette.SLOT_SHADOW);
		context.fill(box.x() + 1, box.y() + 1, box.width() - 2, box.height() - 2, 0xFF000000);
		int x = context.left() + box.x() + 1;
		int y = context.top() + box.y() + 1;
		InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x, y, x + box.width() - 2, y + box.height() - 2,
				MODEL_SCALE, 0.25F, mouseX, mouseY, horse);
	}
}
