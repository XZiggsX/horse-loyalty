package com.xziggsx.horseloyalty.hunger;

import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.TastesLine;
import com.xziggsx.horseloyalty.horseinfo.api.InfoProvider;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Hunger's line on the horse info screen: the foods a tamed horse is known to love and dislike. */
final class HungerInfo implements InfoProvider {
	@Override
	public void contribute(AbstractHorse horse, Player player, HorseProfile.Builder profile) {
		Tastes.Known known = horse.getAttached(Tastes.KNOWN);
		if (!horse.isTamed() || known == null || !HungerSettings.get().preferences()) {
			return;
		}
		profile.tastes(() -> new TastesLine(names(known.loves()), known.hiddenLoves(), names(known.dislikes()), known.hiddenDislikes()));
	}

	@Override
	public List<Object> changeKey(AbstractHorse horse, Player player) {
		Tastes.Known known = horse.getAttached(Tastes.KNOWN);
		return List.of(horse.isTamed(), HungerSettings.get().preferences(), known == null ? "" : known);
	}

	/** The items' names, or the id as written if no such item is known here. */
	private static List<Component> names(List<String> items) {
		return items.stream().map(HungerInfo::name).toList();
	}

	private static Component name(String item) {
		Identifier id = Identifier.tryParse(item);
		if (id == null) {
			return Component.literal(item);
		}
		return BuiltInRegistries.ITEM.getOptional(id)
				.<Component>map(found -> new ItemStack(found).getHoverName())
				.orElse(Component.literal(item));
	}
}
