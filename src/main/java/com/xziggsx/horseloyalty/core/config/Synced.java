package com.xziggsx.horseloyalty.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a settings section the server sends to every player (see {@link SettingsSync}), because
 * the rider's client uses it, such as riding's numbers. Clients then use the server's values, not
 * their own file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Synced {
}
