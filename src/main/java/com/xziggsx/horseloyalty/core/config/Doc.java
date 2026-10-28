package com.xziggsx.horseloyalty.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** What a setting does, in one or two plain sentences. Written to the section's "_readme" in the config file. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Doc {
	/** The explanation. */
	String value();
}
