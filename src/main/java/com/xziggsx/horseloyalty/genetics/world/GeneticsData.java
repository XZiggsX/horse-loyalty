package com.xziggsx.horseloyalty.genetics.world;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.genetics.core.Genome;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * The genetics data saved on a horse. Both are synced to every client, so the dev panel and
 * a future breeding screen can read them there.
 */
public final class GeneticsData {
	/** The horse's genes. Set at birth or spawn, or built to fit its look the first time it's loaded. */
	public static final AttachmentType<Genome> GENOME = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "genome"),
			builder -> builder.persistent(GenomeCodecs.CODEC).syncWith(GenomeCodecs.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/** The horse's parents' genomes and names. Absent on horses from before genetics was on. */
	public static final AttachmentType<Lineage> LINEAGE = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HorseLoyaltyMod.MOD_ID, "lineage"),
			builder -> builder.persistent(Lineage.CODEC).syncWith(Lineage.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	private GeneticsData() {
	}

	/** Forces the class to load so the attachments are registered during mod init. */
	public static void init() {
	}
}
