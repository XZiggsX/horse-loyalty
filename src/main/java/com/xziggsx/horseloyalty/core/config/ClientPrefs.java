package com.xziggsx.horseloyalty.core.config;

import com.xziggsx.horseloyalty.HorseLoyaltyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * A player's personal choices, {@code config/horseloyalty-client.json}: the feedback toggles. Only
 * cosmetic things live here; nothing in it can switch a feature or change how a horse behaves.
 * A dedicated server has no player, so it uses the defaults and writes no file.
 */
public final class ClientPrefs {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String FILE_NAME = HorseLoyaltyMod.MOD_ID + "-client.json";
	private static final int VERSION = 1;

	private static final Map<Class<? extends SettingsSection>, SettingsSection> DEFAULTS = new LinkedHashMap<>();
	private static volatile Map<Class<? extends SettingsSection>, SettingsSection> loaded = Map.of();
	/** The old config's feedback section, kept until the prefs file is written for the first time. */
	private static JsonObject oldFeedback;

	private ClientPrefs() {
	}

	/** Adds a section of personal choices. Call before {@link #load}. */
	public static synchronized void register(SettingsSection defaults) {
		DEFAULTS.put(defaults.getClass(), defaults);
	}

	/** The section, as the player's file says. Defaults until loaded. */
	@SuppressWarnings("unchecked")
	public static <T extends SettingsSection> T section(Class<T> type) {
		SettingsSection section = loaded.get(type);
		return section != null ? (T) section : SettingsResolver.resolve(SettingsResolver.newInstance(type));
	}

	/**
	 * Hands over the "feedback" section of a config from before version 4. It becomes this file's
	 * feedback section if the file doesn't exist yet.
	 */
	static synchronized void adoptOldFeedback(JsonObject feedback) {
		oldFeedback = feedback;
	}

	/** Reads the file, or writes it with the defaults (and any adopted old settings) if it doesn't exist yet. */
	public static synchronized void load() {
		boolean client = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		JsonObject json = client ? ConfigFile.read(path) : new JsonObject();
		boolean fresh = client && !Files.exists(path);
		if (json == null) {
			json = new JsonObject();
			client = false;
		}
		if (fresh && oldFeedback != null) {
			json.add("feedback", oldFeedback);
		}
		JsonObject out = new JsonObject();
		out.addProperty("configVersion", VERSION);
		Map<Class<? extends SettingsSection>, SettingsSection> sections = new LinkedHashMap<>();
		for (SettingsSection defaults : DEFAULTS.values()) {
			JsonElement user = json.get(defaults.key());
			SettingsSection section = ConfigFile.parse(defaults.getClass(), user);
			sections.put(defaults.getClass(), section);
			JsonObject values = GSON.toJsonTree(SettingsResolver.newInstance(defaults.getClass())).getAsJsonObject();
			if (user != null && user.isJsonObject()) {
				values = ConfigFile.merge(values, user.getAsJsonObject());
			}
			JsonObject written = new JsonObject();
			written.addProperty("_readme", SettingsResolver.readme(defaults.getClass()));
			values.entrySet().forEach(entry -> written.add(entry.getKey(), entry.getValue()));
			out.add(defaults.key(), written);
		}
		loaded = Map.copyOf(sections);
		if (client) {
			ConfigFile.write(path, out);
		}
		oldFeedback = null;
	}
}
