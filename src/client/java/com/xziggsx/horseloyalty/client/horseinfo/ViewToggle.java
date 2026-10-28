package com.xziggsx.horseloyalty.client.horseinfo;

import com.xziggsx.horseloyalty.client.horseinfo.InfoLayout.Rect;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * The two tabs on the horse window's top-right edge: Inventory and Info. The one for the view
 * that's showing is lit. They sit in the same place on both windows, so you can switch back
 * and forth without moving the mouse far.
 */
public final class ViewToggle {
	private final Tab inventory;
	private final Tab info;

	/**
	 * @param left        the window's left edge on screen
	 * @param top         the window's top edge on screen
	 * @param windowWidth the window's width
	 * @param infoOpen    whether the info view is showing
	 * @param onChange    called with true to open the info view, false for the inventory
	 */
	public ViewToggle(int left, int top, int windowWidth, boolean infoOpen, Consumer<Boolean> onChange) {
		inventory = new Tab(InfoLayout.inventoryButton(windowWidth), left, top, Icon.INVENTORY,
				Component.translatable("horseloyalty.info.tab.inventory"), () -> onChange.accept(false));
		info = new Tab(InfoLayout.infoButton(windowWidth), left, top, Icon.GENES,
				Component.translatable("horseloyalty.info.tab.info"), () -> onChange.accept(true));
		inventory.selected = !infoOpen;
		info.selected = infoOpen;
	}

	public AbstractButton inventoryTab() {
		return inventory;
	}

	public AbstractButton infoTab() {
		return info;
	}

	/** The Info tab can't be used while an item is on the cursor, so nothing is dropped or hidden. */
	public void setCarrying(boolean carrying) {
		info.active = !carrying || info.selected;
	}

	private enum Icon {
		INVENTORY,
		GENES
	}

	/** One tab: a small lit or plain button with a pixel icon. */
	private static final class Tab extends AbstractButton {
		private static final int FILL = 0xFFA9A9A9;
		private static final int FILL_SELECTED = 0xFF7D9BE0;
		private static final int LIGHT = 0xFFE3E3E3;
		private static final int LIGHT_SELECTED = 0xFFC9D8FF;
		private static final int SHADOW = 0xFF4F4F4F;
		private static final int SHADOW_SELECTED = 0xFF2C3F73;
		private static final int OUTLINE = 0xFF000000;

		private final Icon icon;
		private final Runnable action;
		private boolean selected;

		Tab(Rect rect, int left, int top, Icon icon, Component name, Runnable action) {
			super(left + rect.x(), top + rect.y(), rect.width(), rect.height(), name);
			this.icon = icon;
			this.action = action;
			setTooltip(Tooltip.create(name));
		}

		@Override
		public void onPress(InputWithModifiers input) {
			if (!selected) {
				action.run();
			}
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			int x = getX();
			int y = getY();
			int right = x + getWidth();
			int bottom = y + getHeight();
			boolean lit = selected || (isHoveredOrFocused() && active);
			graphics.fill(x, y, right, bottom, OUTLINE);
			graphics.fill(x + 1, y + 1, right - 1, bottom - 1, selected ? SHADOW_SELECTED : SHADOW);
			graphics.fill(x + 1, y + 1, right - 2, bottom - 2, selected ? LIGHT_SELECTED : LIGHT);
			graphics.fill(x + 2, y + 2, right - 2, bottom - 2, selected ? FILL_SELECTED : lit ? 0xFFBDBDBD : FILL);
			drawIcon(graphics, x + 4, y + 3, active ? 0xFFFFFFFF : 0xFF707070);
		}

		/** A 6 × 7 icon: four squares for the inventory, a twisted ladder for genes. */
		private void drawIcon(GuiGraphicsExtractor graphics, int x, int y, int colour) {
			if (icon == Icon.INVENTORY) {
				graphics.fill(x, y, x + 3, y + 3, colour);
				graphics.fill(x + 4, y, x + 7, y + 3, colour);
				graphics.fill(x, y + 4, x + 3, y + 7, colour);
				graphics.fill(x + 4, y + 4, x + 7, y + 7, colour);
				return;
			}
			int strand = active ? 0xFF3A2A1A : 0xFF606060;
			int[][] left = {{1, 0}, {2, 1}, {3, 2}, {3, 3}, {2, 4}, {1, 5}, {1, 6}};
			int[][] right = {{5, 0}, {4, 1}, {3, 2}, {3, 3}, {4, 4}, {5, 5}, {5, 6}};
			for (int i = 0; i < left.length; i++) {
				graphics.fill(x + left[i][0], y + left[i][1], x + left[i][0] + 1, y + left[i][1] + 1, strand);
				graphics.fill(x + right[i][0], y + right[i][1], x + right[i][0] + 1, y + right[i][1] + 1, strand);
			}
			graphics.fill(x + 2, y + 1, x + 5, y + 2, active ? 0xFFC0392B : colour);
			graphics.fill(x + 2, y + 4, x + 5, y + 5, active ? 0xFF2E6FD1 : colour);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
