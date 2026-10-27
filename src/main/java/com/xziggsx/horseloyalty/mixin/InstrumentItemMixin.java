package com.xziggsx.horseloyalty.mixin;

import com.xziggsx.horseloyalty.horn.HornBinding;
import com.xziggsx.horseloyalty.horn.HornCall;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Calls the bound horse when a bound goat horn is blown (see {@link HornCall}). Runs after vanilla
 * has played the horn and started its cooldown, and only if it did: a horn on cooldown never gets here.
 */
@Mixin(InstrumentItem.class)
public abstract class InstrumentItemMixin {
	@Inject(method = "use", at = @At("RETURN"))
	private void horseloyalty$callHorse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(cir.getReturnValue() instanceof InteractionResult.Success)) {
			return;
		}
		UUID horse = player.getItemInHand(hand).get(HornBinding.BOUND_HORSE);
		if (horse != null) {
			HornCall.call(serverPlayer, horse);
		}
	}
}
