package com.xziggsx.horseloyalty.horseinfo.client.section;

import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import com.xziggsx.horseloyalty.horseinfo.client.InfoPalette;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * What a section needs to draw: the graphics, the font, where the window is on screen, the mouse
 * (for tooltips) and how far the gene table is scrolled. Section boxes are relative to the window.
 */
public record InfoContext(GuiGraphicsExtractor graphics, Font font, int left, int top, int mouseX, int mouseY, int scroll) {
	/** Text at a window-relative position, in the screen's dark label colour. */
	public void text(Component text, int x, int y, int colour) {
		graphics.text(font, text, left + x, top + y, colour, false);
	}

	/** A filled box at a window-relative position. */
	public void fill(int x, int y, int width, int height, int colour) {
		graphics.fill(left + x, top + y, left + x + width, top + y + height, colour);
	}

	/** Whether the mouse is over a window-relative box. */
	public boolean hovering(Rect rect) {
		return rect.contains(mouseX - left, mouseY - top);
	}

	/** One dimmed line saying why a part is locked. */
	public void locked(Component reason, int x, int y) {
		text(reason, x, y, InfoPalette.DIM);
	}
}
