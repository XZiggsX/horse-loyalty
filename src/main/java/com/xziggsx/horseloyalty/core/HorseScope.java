package com.xziggsx.horseloyalty.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;

/**
 * Which horses the mod handles. By default horses, donkeys, mules and the undead horses; llamas and
 * camels are left alone. Data packs change it with the entity type tag {@code horseloyalty:in_scope},
 * so a modded horse can join in.
 */
public final class HorseScope {
	private HorseScope() {
	}

	/**
	 * Whether the mod handles this horse. Works on both sides. A client connected to a server without
	 * the mod never receives the tag, so it falls back to the default kinds instead of handling none.
	 */
	public static boolean inScope(AbstractHorse horse) {
		if (BuiltInRegistries.ENTITY_TYPE.get(HorseTags.IN_SCOPE).isEmpty()) {
			return !(horse instanceof Llama) && !(horse instanceof Camel);
		}
		return horse.getType().builtInRegistryHolder().is(HorseTags.IN_SCOPE);
	}
}
