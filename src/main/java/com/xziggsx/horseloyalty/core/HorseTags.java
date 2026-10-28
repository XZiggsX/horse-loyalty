package com.xziggsx.horseloyalty.core;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Every data pack tag the mod reads, in one place. The default contents are in
 * {@code data/horseloyalty/tags/}, and data packs can add to them or replace them.
 */
public final class HorseTags {
	/** The kinds of horse the mod handles, see {@link HorseScope}. By default horses, donkeys, mules and the undead horses. */
	public static final TagKey<EntityType<?>> IN_SCOPE = TagKey.create(Registries.ENTITY_TYPE, id("in_scope"));
	/** What a skittish horse runs from and bolts at. By default creepers. */
	public static final TagKey<EntityType<?>> SPOOKS_HORSES = TagKey.create(Registries.ENTITY_TYPE, id("spooks_horses"));
	/** Items a tamed horse takes as a treat on top of its own vanilla food. Empty by default. */
	public static final TagKey<Item> TREATS = TagKey.create(Registries.ITEM, id("treats"));
	/** Items that can be bound to a horse and blown to call it. By default goat horns. */
	public static final TagKey<Item> SUMMONING_HORNS = TagKey.create(Registries.ITEM, id("summoning_horns"));
	/** Blocks an unridden horse grazes on, which keeps it fed. By default grass, mycelium and podzol. */
	public static final TagKey<Block> GRAZING_BLOCKS = TagKey.create(Registries.BLOCK, id("grazing_blocks"));

	private HorseTags() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, path);
	}
}
