package com.xziggsx.horseloyalty.genetics.core;

/**
 * A trait with a real name for what the genes give, finer than what the game can draw:
 * a palomino and a buckskin share one coat texture but have different names.
 */
public interface NamedTrait {
	/** The id of the name these genes give, such as "palomino". Used in lang keys. */
	String nameId(Genome genome);
}
