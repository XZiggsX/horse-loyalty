package com.xziggsx.horseloyalty.riding.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Access to a horse's angry sound, so the gait packet handler can play the horse's own voice
 * (it differs for donkeys and the undead horses).
 */
@Mixin(AbstractHorse.class)
public interface HorseSoundInvoker {
	@Invoker("getAngrySound")
	SoundEvent horseloyalty$getAngrySound();
}
