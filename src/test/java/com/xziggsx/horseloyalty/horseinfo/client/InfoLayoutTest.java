package com.xziggsx.horseloyalty.horseinfo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Layout;
import com.xziggsx.horseloyalty.horseinfo.client.InfoLayout.Rect;
import java.util.List;
import org.junit.jupiter.api.Test;

class InfoLayoutTest {
	/** Gene table title plus 8 rows, as for a horse. */
	private static final int HORSE_GENES_HEIGHT = 11 + 8 * 11;

	private static void assertInside(Layout layout, Rect rect, String name) {
		assertTrue(rect.x() >= 0 && rect.y() >= 0 && rect.right() <= layout.width() && rect.bottom() <= layout.height(),
				name + " " + rect + " inside " + layout.width() + " x " + layout.height());
	}

	@Test
	void preferredSizeFitsEveryPartWithoutOverlap() {
		Layout layout = InfoLayout.compute(InfoLayout.PREFERRED_WIDTH, InfoLayout.PREFERRED_HEIGHT, true);
		List<Rect> parts = List.of(layout.model(), layout.side(), layout.looks(), layout.genes());
		for (Rect part : parts) {
			assertInside(layout, part, "part");
		}
		for (int i = 0; i < parts.size(); i++) {
			for (int j = i + 1; j < parts.size(); j++) {
				assertFalse(parts.get(i).overlaps(parts.get(j)), parts.get(i) + " overlaps " + parts.get(j));
			}
		}
		assertTrue(layout.side().x() > layout.model().right(), "the side panel is right of the horse");
		assertTrue(layout.separatorY() >= layout.side().bottom() && layout.separatorY() < layout.looks().y());
		assertTrue(layout.genes().height() >= HORSE_GENES_HEIGHT, "all 8 genes fit without scrolling at the preferred size");
	}

	@Test
	void withoutLooksTheGenesMoveUp() {
		Layout horse = InfoLayout.compute(InfoLayout.PREFERRED_WIDTH, InfoLayout.PREFERRED_HEIGHT, true);
		Layout donkey = InfoLayout.compute(InfoLayout.PREFERRED_WIDTH, InfoLayout.PREFERRED_HEIGHT, false);
		assertEquals(0, donkey.looks().height());
		assertEquals(donkey.looks().y(), donkey.genes().y());
		assertTrue(donkey.genes().y() < horse.genes().y());
	}

	@Test
	void smallScreensFallBackToVanillaSizeAndScroll() {
		assertEquals(InfoLayout.PREFERRED_WIDTH, InfoLayout.infoWidth(480));
		assertEquals(InfoLayout.PREFERRED_HEIGHT, InfoLayout.infoHeight(270), "1080p at GUI scale 4 fits");
		assertEquals(InfoLayout.VANILLA_WIDTH, InfoLayout.infoWidth(150), "never smaller than vanilla");
		assertEquals(InfoLayout.VANILLA_HEIGHT, InfoLayout.infoHeight(100));
		int height = InfoLayout.infoHeight(220);
		assertTrue(height < InfoLayout.PREFERRED_HEIGHT && height >= InfoLayout.VANILLA_HEIGHT);
		Layout small = InfoLayout.compute(InfoLayout.VANILLA_WIDTH, InfoLayout.VANILLA_HEIGHT, true);
		assertInside(small, small.genes(), "genes");
		assertTrue(small.genes().height() >= 11 + 2 * 11, "room for the title and at least two rows to scroll through");
	}

	@Test
	void tabsSitOnTheTopRightEdgeInBothSizes() {
		for (int width : new int[] {InfoLayout.VANILLA_WIDTH, InfoLayout.PREFERRED_WIDTH}) {
			Rect inventory = InfoLayout.inventoryButton(width);
			Rect info = InfoLayout.infoButton(width);
			assertTrue(info.x() > inventory.right() - 1, "Info is right of Inventory");
			assertTrue(info.right() <= width, "inside the window's width");
			assertTrue(info.y() < 0 && info.bottom() > 0, "on the top edge");
			assertFalse(info.overlaps(inventory));
			assertEquals(width - info.right(), InfoLayout.PADDING, "same distance from the right edge");
		}
	}
}
