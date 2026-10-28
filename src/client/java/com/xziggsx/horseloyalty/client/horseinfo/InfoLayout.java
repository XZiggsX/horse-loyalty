package com.xziggsx.horseloyalty.client.horseinfo;

/**
 * Where everything goes on the horse info screen, in GUI pixels from the window's top-left
 * corner. Pure arithmetic, so it can be tested without the game. To move or resize a part of
 * the screen, change it here; the sections draw into whatever box they're given.
 */
public final class InfoLayout {
	/** The info window's size when the screen has room; vanilla's horse window is 176 × 166. */
	public static final int PREFERRED_WIDTH = 256;
	public static final int PREFERRED_HEIGHT = 222;
	public static final int VANILLA_WIDTH = 176;
	public static final int VANILLA_HEIGHT = 166;

	static final int PADDING = 7;
	static final int BUTTON_WIDTH = 14;
	static final int BUTTON_HEIGHT = 13;
	/** The buttons sit on the window's top edge like tabs; this much of them is above it. */
	static final int TAB_RISE = 12;
	/** Room kept free above and below the window, for the tabs and a margin. */
	static final int SCREEN_MARGIN = TAB_RISE + 4;

	/** Width of the label column beside the model, shared by the stat bars and the parents line. */
	public static final int LABEL_WIDTH = 42;

	static final int MODEL_SIZE = 54;
	static final int SIDE_HEIGHT = 64;
	static final int LOOKS_HEIGHT = 22;
	static final int GAP = 4;

	private InfoLayout() {
	}

	/** A box, relative to the window's top-left corner. */
	public record Rect(int x, int y, int width, int height) {
		public int right() {
			return x + width;
		}

		public int bottom() {
			return y + height;
		}

		public boolean contains(double px, double py) {
			return px >= x && px < right() && py >= y && py < bottom();
		}

		public boolean overlaps(Rect other) {
			return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
		}
	}

	/**
	 * Every part of the info view.
	 *
	 * @param model      the black box the horse is drawn in
	 * @param side       right of the model: header, stats and parents, top to bottom
	 * @param separatorY the line between the top part and the looks and genes
	 * @param looks      the coat and markings; zero height for donkeys and mules
	 * @param genes      the gene table, which scrolls when it doesn't fit
	 */
	public record Layout(int width, int height, Rect model, Rect side, int separatorY, Rect looks, Rect genes) {
	}

	/** The info window's width on a screen this wide: the preferred width, less if the screen is narrow, never below vanilla's. */
	public static int infoWidth(int screenWidth) {
		return Math.max(VANILLA_WIDTH, Math.min(PREFERRED_WIDTH, screenWidth - 2 * SCREEN_MARGIN));
	}

	/** The info window's height on a screen this tall, like {@link #infoWidth}. */
	public static int infoHeight(int screenHeight) {
		return Math.max(VANILLA_HEIGHT, Math.min(PREFERRED_HEIGHT, screenHeight - 2 * SCREEN_MARGIN));
	}

	/** The Inventory tab, for a window this wide. Same spot in both views. */
	public static Rect inventoryButton(int windowWidth) {
		return new Rect(windowWidth - PADDING - 2 * BUTTON_WIDTH - 1, -TAB_RISE, BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	/** The Info tab, right of the Inventory tab. */
	public static Rect infoButton(int windowWidth) {
		return new Rect(windowWidth - PADDING - BUTTON_WIDTH, -TAB_RISE, BUTTON_WIDTH, BUTTON_HEIGHT);
	}

	/** The info view for a window of this size. Without looks, the genes move up into their place. */
	public static Layout compute(int width, int height, boolean hasLooks) {
		int inner = width - 2 * PADDING;
		Rect model = new Rect(PADDING, 17, MODEL_SIZE, MODEL_SIZE);
		int sideX = model.right() + 6;
		Rect side = new Rect(sideX, model.y(), width - PADDING - sideX, SIDE_HEIGHT);
		int separatorY = side.bottom() + 3;
		int below = separatorY + 4;
		Rect looks = new Rect(PADDING, below, inner, hasLooks ? LOOKS_HEIGHT : 0);
		int genesY = hasLooks ? looks.bottom() + GAP : below;
		Rect genes = new Rect(PADDING, genesY, inner, Math.max(0, height - PADDING - genesY));
		return new Layout(width, height, model, side, separatorY, looks, genes);
	}
}
