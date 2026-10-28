package com.xziggsx.horseloyalty.core.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Makes a settings object safe to use: fills in defaults for missing values and clamps numbers to
 * their {@link Range}. Works field by field on any plain settings class, nested objects included,
 * so no settings class needs its own clamping code. A missing value (null, or a number that isn't
 * one) takes the value from a fresh instance of the class, which holds the defaults.
 */
public final class SettingsResolver {
	private SettingsResolver() {
	}

	/** Fills in defaults and clamps every value, then lets the section work out its derived values. */
	public static <T extends SettingsSection> T resolve(T section) {
		fill(section, newInstance(section.getClass()));
		section.resolve();
		return section;
	}

	/** One line per documented setting, such as "mutationPercent: Chance that ...", for the section's readme. */
	public static String readme(Class<?> type) {
		List<String> lines = new ArrayList<>();
		addDocs(type, "", lines);
		return String.join(" ", lines);
	}

	private static void addDocs(Class<?> type, String prefix, List<String> lines) {
		for (Field field : settingsFields(type)) {
			Doc doc = field.getAnnotation(Doc.class);
			if (doc != null) {
				lines.add(prefix + field.getName() + ": " + doc.value());
			}
			if (isNested(field.getType())) {
				addDocs(field.getType(), prefix + field.getName() + ".", lines);
			}
		}
	}

	/** Copies defaults into missing values of {@code target} and clamps it, recursing into nested settings. */
	private static void fill(Object target, Object defaults) {
		for (Field field : settingsFields(target.getClass())) {
			try {
				Object value = field.get(target);
				Object fallback = field.get(defaults);
				if (value == null || isNotANumber(value)) {
					field.set(target, fallback);
					continue;
				}
				Range range = field.getAnnotation(Range.class);
				if (range != null) {
					field.set(target, clamp(value, range));
				} else if (isNested(field.getType()) && fallback != null) {
					fill(value, fallback);
				} else if (value instanceof Collection<?> collection) {
					clampElements(collection);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Can't read setting " + field, e);
			}
		}
	}

	/** Lists of nested settings get each element filled in from a fresh element. */
	private static void clampElements(Collection<?> collection) {
		for (Object element : collection) {
			if (element != null && isNested(element.getClass())) {
				fill(element, newInstance(element.getClass()));
			}
		}
	}

	private static Object clamp(Object value, Range range) {
		return switch (value) {
			case Double number -> Math.clamp(number, range.min(), range.max());
			case Float number -> (float) Math.clamp(number, range.min(), range.max());
			case Integer number -> (int) Math.clamp(number.longValue(), (long) Math.ceil(range.min()), (long) Math.floor(range.max()));
			case Long number -> Math.clamp(number, (long) Math.ceil(range.min()), (long) Math.floor(range.max()));
			default -> value;
		};
	}

	private static boolean isNotANumber(Object value) {
		return value instanceof Double number && (number.isNaN() || number.isInfinite())
				|| value instanceof Float single && (single.isNaN() || single.isInfinite());
	}

	/** A field holding another settings object, rather than a value. */
	private static boolean isNested(Class<?> type) {
		return !type.isPrimitive() && !type.isEnum() && !type.isArray() && !type.getName().startsWith("java.")
				&& !Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type);
	}

	/** The JSON fields of a settings class: not static, not transient. */
	private static List<Field> settingsFields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Field field : type.getDeclaredFields()) {
			int modifiers = field.getModifiers();
			if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
				continue;
			}
			field.setAccessible(true);
			fields.add(field);
		}
		return fields;
	}

	/** A fresh instance with every default, from the class's no-argument constructor. */
	static <T> T newInstance(Class<T> type) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(type + " needs a no-argument constructor", e);
		}
	}
}
