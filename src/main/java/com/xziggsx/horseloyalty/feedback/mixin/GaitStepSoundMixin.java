package com.xziggsx.horseloyalty.feedback.mixin;

import com.xziggsx.horseloyalty.feedback.GaitSounds;
import com.xziggsx.horseloyalty.riding.api.Gait;
import com.xziggsx.horseloyalty.riding.api.RideView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hoofbeats that match the gait, see {@link GaitSounds}. Only for a horse whose rider sends a gait;
 * anything else keeps the vanilla sounds. Runs wherever vanilla plays step sounds.
 */
@Mixin(AbstractHorse.class)
public abstract class GaitStepSoundMixin {
	@Shadow
	protected abstract void playGallopSound(SoundType soundType);

	@Shadow
	private boolean isWoodSoundType(SoundType soundType) {
		return false;
	}

	@Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
	private void horseloyalty$gaitStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
		AbstractHorse horse = (AbstractHorse) (Object) this;
		Gait gait = RideView.get().syncedGait(horse);
		if (gait != null && GaitSounds.play(horse, gait, pos, state, type -> playGallopSound(type), type -> isWoodSoundType(type))) {
			ci.cancel();
		}
	}
}
