package com.xziggsx.horseloyalty.horn;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.HorseScope;
import com.xziggsx.horseloyalty.core.feature.FeatureContext;
import com.xziggsx.horseloyalty.loyalty.Loyalty;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Binding a goat horn to a horse: sneak and use the horn on a tamed horse. The horn remembers the
 * horse (see {@link #BOUND_HORSE}), shines like an enchanted item and, if the horse has a name,
 * takes that name. Blowing the horn calls the horse, see {@link HornCall}.
 * <p>
 * Vanilla opens a tamed horse's inventory when you sneak and click it, before the held item gets
 * a say, so this hooks in earlier with Fabric's {@link UseEntityCallback}. Sneak-clicking with
 * anything other than a goat horn still opens the inventory.
 */
public final class HornBinding {
	/** The horse a horn calls. Saved with the item and synced to clients. */
	public static final DataComponentType<UUID> BOUND_HORSE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "bound_horse"),
			DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

	/** Lore is italic purple by default; plain grey reads as a note rather than flavour text. */
	private static final Style LORE_STYLE = Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY);

	private HornBinding() {
	}

	/** Registers the data component, by loading the class. Runs whether summoning is on or off. */
	public static void init() {
	}

	/** Hooks up the sneak-click that binds a horn. */
	public static void wire(FeatureContext context) {
		context.on(UseEntityCallback.EVENT, HornBinding::onUseEntity);
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.GOAT_HORN) || !player.isSecondaryUseActive() || player.isSpectator()
				|| !(entity instanceof AbstractHorse horse) || !horse.isTamed() || !HorseScope.inScope(horse)) {
			return InteractionResult.PASS;
		}
		// From here the horn handles the click, so the horse's inventory doesn't open as well.
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		int loyalty = Loyalty.level(horse, serverPlayer);
		if (loyalty < HornCall.MIN_LEVEL) {
			serverPlayer.sendOverlayMessage(Component.translatable("horseloyalty.horn.not_trusted",
					horse.getName(), HornCall.MIN_LEVEL, Loyalty.MAX_LEVEL, Loyalty.levelName(HornCall.MIN_LEVEL)));
			return InteractionResult.FAIL;
		}
		bind(stack, horse);
		serverPlayer.sendOverlayMessage(Component.translatable("horseloyalty.horn.bound", horse.getName()));
		// Entity event 7 is the vanilla "tamed" heart burst.
		serverLevel.broadcastEntityEvent(horse, (byte) 7);
		horse.playAmbientSound();
		return InteractionResult.SUCCESS;
	}

	/** Points the horn at this horse: remembers it, adds the glint, and takes its name if it has one. */
	private static void bind(ItemStack stack, AbstractHorse horse) {
		stack.set(BOUND_HORSE, horse.getUUID());
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		if (horse.hasCustomName()) {
			stack.set(DataComponents.ITEM_NAME, Component.translatable("horseloyalty.horn.name", horse.getCustomName()));
		} else {
			// Rebound from a named horse to an unnamed one: back to "Goat Horn".
			stack.remove(DataComponents.ITEM_NAME);
		}
		stack.set(DataComponents.LORE, new ItemLore(List.of(
				Component.translatable("horseloyalty.horn.lore", horse.getName()).setStyle(LORE_STYLE))));
	}
}
