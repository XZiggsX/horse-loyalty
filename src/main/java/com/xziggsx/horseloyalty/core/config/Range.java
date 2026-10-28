package com.xziggsx.horseloyalty.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The limits of a number in a settings section. The {@link SettingsResolver} clamps the value
 * into them, so a typo in the config can't break the game.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {
	/** The lowest allowed value. */
	double min();

	/** The highest allowed value. */
	double max();
}
