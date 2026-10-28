package com.xziggsx.horseloyalty.core.config;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.xziggsx.horseloyalty.core.feature.Feature;
import com.xziggsx.horseloyalty.core.feature.FeatureSwitch;
import com.xziggsx.horseloyalty.core.feature.Features;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The server config, {@code config/horseloyalty.json}: a "features" block with every switch, and
 * one section per feature (see {@link SettingsSection}). Written with the defaults on first launch;
 * new settings are added to an existing file without touching what's there. A file from before
 * version 4 is split up by {@link ConfigMigration} first.
 * <p>
 * A world can override any value with {@code <world>/serverconfig/horseloyalty.json}, read on top
 * of the global file key by key while that world runs. A client connected to a server with the mod
 * uses the server's {@link Synced} sections instead of its own (see {@link SettingsSync}).
 */
public final class ConfigFile {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
	/** Compact, for sending sections to clients. */
	private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();
	private static final String FILE_NAME = HorseLoyaltyMod.MOD_ID + ".json";

	/** A fresh default instance of every section, by class, in the order the features were registered. */
	private static final Map<Class<? extends SettingsSection>, SettingsSection> DEFAULTS = new LinkedHashMap<>();
	/** The sections as this side's own files say, resolved. */
	private static volatile Map<Class<? extends SettingsSection>, SettingsSection> local = Map.of();
	/** Client: the server's synced sections, or empty while not connected to a server with the mod. */
	private static volatile Map<Class<? extends SettingsSection>, SettingsSection> remote = Map.of();
	/** The world override file of the running server, or null. */
	private static Path worldFile;

	private ConfigFile() {
	}

	/** Adds the sections of every registered feature. Call once, after registering the features. */
	public static synchronized void registerSections() {
		for (Feature feature : Features.all()) {
			SettingsSection section = feature.settings();
			if (section != null) {
				DEFAULTS.put(section.getClass(), section);
			}
		}
	}

	/**
	 * The section in use: on a client connected to a server with the mod the server's copy of a
	 * synced section, else this side's own. Defaults until the config is loaded.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends SettingsSection> T section(Class<T> type) {
		SettingsSection section = remote.get(type);
		if (section == null) {
			section = local.get(type);
		}
		if (section == null) {
			// Before load (or a section nobody registered): plain defaults.
			section = SettingsResolver.resolve(SettingsResolver.newInstance(type));
		}
		return (T) section;
	}

	/** Reads the global file (and the running world's override), migrating and completing the global file. */
	public static synchronized void load() {
		Path path = globalPath();
		JsonObject global = read(path);
		boolean broken = global == null;
		if (broken) {
			// Keep the user's broken file so they can fix it; just run on defaults.
			global = new JsonObject();
		} else if (!global.isEmpty() && ConfigMigration.needed(global)) {
			ConfigMigration.Result migrated = ConfigMigration.fromV3(global);
			global = migrated.config();
			if (migrated.feedback() != null) {
				ClientPrefs.adoptOldFeedback(migrated.feedback());
			}
			HorseLoyaltyMod.LOGGER.info("Updated config to version {}: one section per feature", ConfigMigration.VERSION);
		}
		JsonObject complete = complete(global);
		if (!broken) {
			write(path, complete);
		}
		JsonObject effective = complete;
		JsonObject override = worldFile == null ? null : read(worldFile);
		if (override != null) {
			effective = merge(complete.deepCopy(), override);
		}
		apply(effective);
	}

	/** Server starting: reads the world's override on top of the global file. */
	public static void useWorld(MinecraftServer server) {
		worldFile = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(FILE_NAME);
		if (Files.exists(worldFile)) {
			HorseLoyaltyMod.LOGGER.info("Using the world's config override {}", worldFile);
		}
		load();
	}

	/** Server stopped: back to the global file alone. */
	public static void leaveWorld() {
		worldFile = null;
		load();
	}

	/**
	 * Saves a feature switch where it takes effect: the world override if it has a features block,
	 * else the global file. Then reloads.
	 */
	public static synchronized void saveSwitch(String id, FeatureSwitch value) {
		Path target = globalPath();
		if (worldFile != null) {
			JsonObject override = read(worldFile);
			if (override != null && override.has("features")) {
				target = worldFile;
			}
		}
		JsonObject json = read(target);
		if (json == null) {
			json = new JsonObject();
		}
		JsonObject features = json.has("features") && json.get("features").isJsonObject()
				? json.getAsJsonObject("features") : new JsonObject();
		features.addProperty(id, value.id());
		json.add("features", features);
		write(target, json);
		load();
	}

	/** Client: use the server's synced sections (as JSON by key), or this side's own again with null. */
	public static void useRemote(Map<String, String> sections) {
		if (sections == null) {
			remote = Map.of();
			return;
		}
		Map<Class<? extends SettingsSection>, SettingsSection> parsed = new HashMap<>();
		for (SettingsSection defaults : DEFAULTS.values()) {
			String json = sections.get(defaults.key());
			if (json != null && defaults.getClass().isAnnotationPresent(Synced.class)) {
				parsed.put(defaults.getClass(), parse(defaults.getClass(), JsonParser.parseString(json)));
			}
		}
		remote = Map.copyOf(parsed);
	}

	/** This side's own synced sections as JSON by key, for sending to clients. */
	public static Map<String, String> syncedSections() {
		Map<String, String> result = new LinkedHashMap<>();
		for (SettingsSection section : local.values()) {
			if (section.getClass().isAnnotationPresent(Synced.class)) {
				result.put(section.key(), COMPACT.toJson(section));
			}
		}
		return result;
	}

	/** Parses the sections and switches out of a complete config. */
	private static void apply(JsonObject json) {
		Map<Class<? extends SettingsSection>, SettingsSection> sections = new LinkedHashMap<>();
		for (SettingsSection defaults : DEFAULTS.values()) {
			sections.put(defaults.getClass(), parse(defaults.getClass(), json.get(defaults.key())));
		}
		local = Map.copyOf(sections);
		Features.useLocal(switches(json));
	}

	private static Map<String, FeatureSwitch> switches(JsonObject json) {
		Map<String, FeatureSwitch> switches = new LinkedHashMap<>();
		JsonElement block = json.get("features");
		if (block == null || !block.isJsonObject()) {
			return switches;
		}
		for (Map.Entry<String, JsonElement> entry : block.getAsJsonObject().entrySet()) {
			FeatureSwitch value = entry.getValue().isJsonPrimitive() ? FeatureSwitch.byId(entry.getValue().getAsString()) : null;
			if (value == null) {
				HorseLoyaltyMod.LOGGER.warn("Feature {} should be \"on\" or \"off\", not {}; it stays on", entry.getKey(), entry.getValue());
			} else {
				switches.put(entry.getKey(), value);
			}
		}
		return switches;
	}

	/** One section from its JSON, migrated to its version, with defaults filled in and values clamped. */
	static <T extends SettingsSection> T parse(Class<T> type, JsonElement json) {
		T defaults = SettingsResolver.newInstance(type);
		if (json == null || !json.isJsonObject()) {
			return SettingsResolver.resolve(defaults);
		}
		JsonObject object = json.getAsJsonObject().deepCopy();
		int version = object.has("_version") ? object.get("_version").getAsInt() : 1;
		if (version < defaults.version()) {
			defaults.migrate(object, version);
		}
		try {
			T read = GSON.fromJson(object, type);
			return SettingsResolver.resolve(read != null ? read : defaults);
		} catch (JsonParseException | IllegalStateException | NumberFormatException e) {
			HorseLoyaltyMod.LOGGER.error("Could not read the {} section of {}, using its defaults: {}", defaults.key(), FILE_NAME, e.getMessage());
			return SettingsResolver.resolve(defaults);
		}
	}

	/**
	 * The file as it should be written: the user's values over every default, a readme per section,
	 * and a switch for every feature. Values the user set are never changed, only completed.
	 */
	private static JsonObject complete(JsonObject user) {
		JsonObject out = new JsonObject();
		out.addProperty("configVersion", ConfigMigration.VERSION);
		JsonObject features = new JsonObject();
		JsonObject userFeatures = user.has("features") && user.get("features").isJsonObject()
				? user.getAsJsonObject("features") : new JsonObject();
		for (Feature feature : Features.all()) {
			JsonElement value = userFeatures.get(feature.id());
			features.add(feature.id(), value != null ? value : GSON.toJsonTree(FeatureSwitch.ON.id()));
		}
		// Keep switches for features this version doesn't know (an add-on that isn't installed).
		userFeatures.entrySet().forEach(entry -> {
			if (!features.has(entry.getKey())) {
				features.add(entry.getKey(), entry.getValue());
			}
		});
		out.add("features", features);
		for (SettingsSection defaults : DEFAULTS.values()) {
			JsonObject section = new JsonObject();
			section.addProperty("_readme", SettingsResolver.readme(defaults.getClass()));
			section.addProperty("_version", defaults.version());
			JsonObject values = GSON.toJsonTree(SettingsResolver.newInstance(defaults.getClass())).getAsJsonObject();
			JsonElement userSection = user.get(defaults.key());
			if (userSection != null && userSection.isJsonObject()) {
				JsonObject userValues = userSection.getAsJsonObject().deepCopy();
				userValues.remove("_readme");
				userValues.remove("_version");
				values = merge(values, userValues);
			}
			values.entrySet().forEach(entry -> section.add(entry.getKey(), entry.getValue()));
			out.add(defaults.key(), section);
		}
		return out;
	}

	/** {@code base} with every value of {@code over} on top, merging nested objects key by key. */
	static JsonObject merge(JsonObject base, JsonObject over) {
		for (Map.Entry<String, JsonElement> entry : over.entrySet()) {
			JsonElement current = base.get(entry.getKey());
			if (current != null && current.isJsonObject() && entry.getValue().isJsonObject()) {
				merge(current.getAsJsonObject(), entry.getValue().getAsJsonObject());
			} else {
				base.add(entry.getKey(), entry.getValue().deepCopy());
			}
		}
		return base;
	}

	private static Path globalPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** The file as JSON, an empty object if it doesn't exist, or null if it can't be read. */
	static JsonObject read(Path path) {
		if (!Files.exists(path)) {
			return new JsonObject();
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement element = JsonParser.parseReader(reader);
			return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
		} catch (IOException | JsonParseException e) {
			HorseLoyaltyMod.LOGGER.error("Could not read {}, using defaults: {}", path, e.getMessage());
			return null;
		}
	}

	/** Writes the file if its content changed. */
	static void write(Path path, JsonObject json) {
		String text = GSON.toJson(json);
		try {
			if (Files.exists(path) && Files.readString(path).equals(text)) {
				return;
			}
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				writer.write(text);
			}
		} catch (IOException e) {
			HorseLoyaltyMod.LOGGER.warn("Could not write config {}: {}", path, e.getMessage());
		}
	}
}
