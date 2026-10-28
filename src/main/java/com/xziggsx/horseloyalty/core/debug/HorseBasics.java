package com.xziggsx.horseloyalty.core.debug;

import com.xziggsx.horseloyalty.core.HorseUnits;
import java.util.Locale;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/** Core's debug section, always shown first: who the horse is, its owner and its stats. */
public final class HorseBasics implements DebugSource {
	@Override
	public DebugSection section(AbstractHorse horse, AbstractHorse serverHorse, Player player) {
		boolean riding = player != null && player.getVehicle() == horse;
		DebugSection section = new DebugSection("Horse  [dev, " + (riding ? "riding" : "looking") + "]");
		section.add("Name", horse.getName().getString() + "  (" + horse.getType().getDescription().getString() + ")");
		section.add("Tamed", horse.isTamed() ? "yes, owner " + ownerText(player, serverHorse) : "no",
				horse.isTamed() ? DebugTone.NORMAL : DebugTone.DIM);
		section.add("Health", String.format(Locale.ROOT, "%.1f / %.1f", horse.getHealth(), horse.getMaxHealth()));
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		section.add("Speed", String.format(Locale.ROOT, "%.4f  (%.1f b/s)", speed, HorseUnits.blocksPerSecond(speed)));
		section.add("Jump", String.format(Locale.ROOT, "%.3f", horse.getAttributeValue(Attributes.JUMP_STRENGTH)));
		return section;
	}

	/** Whether vanilla's owner (not synced to clients) is you, from the server copy. */
	private static String ownerText(Player player, AbstractHorse serverHorse) {
		if (serverHorse == null) {
			return "unknown";
		}
		EntityReference<LivingEntity> owner = serverHorse.getOwnerReference();
		return owner == null ? "none" : player != null && owner.matches(player) ? "you" : "someone else";
	}
}
