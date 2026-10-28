package com.xziggsx.horseloyalty.loyalty;

import com.xziggsx.horseloyalty.core.debug.DebugSection;
import com.xziggsx.horseloyalty.core.debug.DebugSource;
import com.xziggsx.horseloyalty.core.debug.DebugTone;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile;
import com.xziggsx.horseloyalty.horseinfo.api.HorseProfile.LoyaltyLine;
import com.xziggsx.horseloyalty.horseinfo.api.InfoProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** Loyalty's line on the horse info screen, and its section in the dev HUD and {@code /hl info}. */
final class LoyaltyInfo implements InfoProvider, DebugSource {
	@Override
	public void contribute(AbstractHorse horse, Player player, HorseProfile.Builder profile) {
		profile.loyalty(() -> {
			int level = Loyalty.level(horse, player);
			return new LoyaltyLine(level, Loyalty.MAX_LEVEL, Loyalty.levelName(level));
		});
	}

	@Override
	public List<Object> changeKey(AbstractHorse horse, Player player) {
		return List.of(Loyalty.level(horse, player));
	}

	/** Your loyalty with the horse. Hidden for an untamed horse; the stats need the server's copy. */
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		if (!horse.isTamed() || player == null) {
			return null;
		}
		DebugSection section = new DebugSection("Loyalty");
		int level = Loyalty.level(horse, player);
		section.add("Level", level + "/" + Loyalty.MAX_LEVEL + "  " + Loyalty.levelName(level).getString());
		if (serverHorse == null) {
			return section.add("Stats", "singleplayer only", DebugTone.DIM);
		}
		LoyaltySettings settings = LoyaltySettings.get();
		Map<UUID, LoyaltyData> riders = serverHorse.getAttachedOrElse(Loyalty.DATA, Map.of());
		LoyaltyData data = riders.getOrDefault(player.getUUID(), LoyaltyData.EMPTY);
		int earned = Loyalty.earnedLevel(data);
		section.add("Points", data.points() + (earned >= Loyalty.MAX_LEVEL ? "  (max)"
				: "  (next at " + settings.pointsNeeded(earned + 1) + ")"));
		if (data.lostPoints() > 0) {
			section.add("Lost", data.lostPoints() + " to hunger", DebugTone.WARN);
		}
		long sinceFed = serverHorse.level().getGameTime() - data.lastFedTick();
		long cooldown = data.lastFedTick() == 0L ? 0 : Math.max(0, settings.feedCooldownTicks() - sinceFed);
		section.add("Fed", data.feeds() + "x, " + (cooldown == 0 ? "treat ready" : "next treat in " + DebugSection.seconds(cooldown)),
				cooldown == 0 ? DebugTone.GOOD : DebugTone.NORMAL);
		section.add("Ridden", DebugSection.seconds(data.rideTicks()));
		section.add("Riders", riders.size() + "/" + Loyalty.MAX_REMEMBERED_RIDERS + " remembered");
		return section;
	}
}
