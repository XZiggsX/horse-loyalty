package com.xziggsx.horseloyalty.core.debug;

/** How a debug row's value reads: the dev HUD gives each its own colour. */
public enum DebugTone {
	/** A plain value. */
	NORMAL,
	/** Off, missing or not applicable. */
	DIM,
	/** Something to look at, such as a hungry horse. */
	WARN,
	/** Something ready, such as a treat that will count. */
	GOOD
}
