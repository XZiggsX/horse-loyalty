package com.xziggsx.horseloyalty.core;

import com.xziggsx.horseloyalty.core.link.Contributions;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/**
 * Items a tamed horse can be fed on top of its own vanilla food. The {@code horseloyalty:treats}
 * tag always counts; features add their own, such as hunger's feedable foods from the config.
 */
public final class Treats {
	/** The extra edible items. Add to it with {@code context.contribute(Treats.ALL, predicate)}. */
	public static final Contributions<Predicate<ItemStack>> ALL = new Contributions<>("core:treats");

	static {
		ALL.add(null, stack -> stack.is(HorseTags.TREATS));
	}

	private Treats() {
	}

	/** Whether a tamed horse may be fed this, besides its vanilla food. */
	public static boolean isTreat(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		for (Predicate<ItemStack> treat : ALL.active()) {
			if (treat.test(stack)) {
				return true;
			}
		}
		return false;
	}
}
