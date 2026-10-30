package com.xziggsx.horseloyalty.horseinfo.api;

/** The parts of the horse info screen a {@link RevealPolicy} can show or lock. */
public enum InfoTopic {
	/** Kind and personality. */
	IDENTITY,
	/** Your loyalty level with the horse. */
	LOYALTY,
	/** Health, speed and jump. */
	STATS,
	/** The parents' names. */
	PARENTS,
	/** The real names of the coat and markings. */
	LOOKS,
	/** The gene table. */
	GENES,
	/** The foods the horse is known to love and dislike. */
	TASTES
}
