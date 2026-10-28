package com.xziggsx.horseloyalty.core;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Every data pack tag the mod reads, in one place. The default contents are in
 * {@code data/horseloyalty/tags/}, and data packs can add to them or replace them.
 */
public final class HorseTags {
	/** The kinds of horse the mod handles, see {@link HorseScope}. */
	public static final TagKey<EntityType<?>> IN_SCOPE = TagKey.create(Registries.ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "in_scope"));

	private HorseTags() {
	}
}
