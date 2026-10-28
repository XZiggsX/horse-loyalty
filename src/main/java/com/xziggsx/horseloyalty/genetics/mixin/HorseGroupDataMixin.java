package com.xziggsx.horseloyalty.genetics.mixin;

import com.xziggsx.horseloyalty.genetics.world.HerdFounders;
import net.minecraft.world.entity.animal.equine.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets a horse spawn group remember its herd's founders (see {@link HerdFounders}). */
@Mixin(Horse.HorseGroupData.class)
public abstract class HorseGroupDataMixin implements HerdFounders.Holder {
	@Unique
	private HerdFounders horseloyalty$founders;

	@Override
	public HerdFounders horseloyalty$getFounders() {
		return horseloyalty$founders;
	}

	@Override
	public void horseloyalty$setFounders(HerdFounders founders) {
		horseloyalty$founders = founders;
	}
}
