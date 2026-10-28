package com.xziggsx.horseloyalty.genetics.mixin;

import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Access to a horse's private coat setter, so genetics can set the coat and markings its genes show. */
@Mixin(Horse.class)
public interface HorseInvoker {
	@Invoker("setVariantAndMarkings")
	void horseloyalty$setVariantAndMarkings(Variant variant, Markings markings);
}
