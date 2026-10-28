package com.xziggsx.horseloyalty.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Keeps the features apart. A feature package (in both source sets) may import only core, its own
 * package and other features' {@code api} packages; core may import no feature at all. Only the two
 * entrypoints may import everything, since they list the features.
 */
class ImportBoundaryTest {
	private static final String ROOT = "com.xziggsx.horseloyalty";
	private static final Set<String> FEATURES = Set.of("riding", "loyalty", "hunger", "personality", "genetics",
			"foals", "summoning", "feedback", "horseinfo", "dev");
	/** The entrypoints, which register every feature. */
	private static final Set<String> ENTRYPOINTS = Set.of(ROOT + ".HorseLoyaltyMod", ROOT + ".client.HorseLoyaltyClient");
	/** Classes every package may use: the mod id and logger. */
	private static final Set<String> SHARED = Set.of(ROOT + ".HorseLoyaltyMod");
	/** Feature classes still allowed to cross a boundary until their part of the migration is done. */
	private static final Set<String> NOT_MIGRATED_YET = Set.of();

	private static final Pattern IMPORT = Pattern.compile("^import (?:static )?(" + Pattern.quote(ROOT) + "[\\w.]*?)(?:\\.\\*)?;",
			Pattern.MULTILINE);

	@Test
	void featuresOnlyImportCoreAndApis() throws IOException {
		List<String> violations = new ArrayList<>();
		for (Path file : javaFiles()) {
			String className = className(file);
			String owner = ownerOf(packageOf(className));
			if (owner == null || ENTRYPOINTS.contains(className) || NOT_MIGRATED_YET.contains(className)) {
				continue;
			}
			for (String imported : imports(file)) {
				if (!allowed(owner, imported)) {
					violations.add(className + " imports " + imported);
				}
			}
		}
		assertTrue(violations.isEmpty(), "Imports across feature boundaries:\n" + String.join("\n", violations));
	}

	@Test
	void everyClassBelongsToCoreOrAFeature() throws IOException {
		List<String> strays = new ArrayList<>();
		for (Path file : javaFiles()) {
			String className = className(file);
			if (ownerOf(packageOf(className)) == null && !ENTRYPOINTS.contains(className)) {
				strays.add(className);
			}
		}
		assertTrue(strays.isEmpty(), "Classes outside core and the feature packages:\n" + String.join("\n", strays));
	}

	/** Whether code owned by {@code owner} ("core" or a feature) may import from this package or class. */
	private static boolean allowed(String owner, String imported) {
		if (SHARED.stream().anyMatch(imported::startsWith)) {
			return true;
		}
		String importedOwner = ownerOf(imported);
		if (importedOwner == null || importedOwner.equals(owner) || importedOwner.equals("core")) {
			return importedOwner != null || !owner.equals("core");
		}
		// Another feature: only its api package.
		return !owner.equals("core") && isApi(imported, importedOwner);
	}

	private static boolean isApi(String imported, String feature) {
		String base = ROOT + "." + feature + ".api";
		return imported.equals(base) || imported.startsWith(base + ".");
	}

	/** "core", a feature id, or null for a legacy package or the root. */
	private static String ownerOf(String name) {
		if (!name.startsWith(ROOT + ".")) {
			return null;
		}
		String rest = name.substring(ROOT.length() + 1);
		String first = rest.contains(".") ? rest.substring(0, rest.indexOf('.')) : rest;
		if (first.equals("core") || FEATURES.contains(first)) {
			return first;
		}
		return null;
	}

	/** The full names imported by a file. */
	private static List<String> imports(Path file) {
		List<String> result = new ArrayList<>();
		Matcher matcher = IMPORT.matcher(read(file));
		while (matcher.find()) {
			result.add(matcher.group(1));
		}
		return result;
	}

	private static List<Path> javaFiles() throws IOException {
		List<Path> files = new ArrayList<>();
		for (String sourceSet : List.of("src/main/java", "src/client/java")) {
			Path root = Path.of(sourceSet);
			try (Stream<Path> walk = Files.walk(root)) {
				walk.filter(path -> path.toString().endsWith(".java")).forEach(files::add);
			}
		}
		assertTrue(!files.isEmpty(), "No sources found; the test must run from the project folder");
		return files;
	}

	private static String className(Path file) {
		String text = read(file);
		Matcher matcher = Pattern.compile("^package ([\\w.]+);", Pattern.MULTILINE).matcher(text);
		String pkg = matcher.find() ? matcher.group(1) : "";
		String name = file.getFileName().toString().replace(".java", "");
		return pkg + "." + name;
	}

	private static String packageOf(String className) {
		return className.substring(0, className.lastIndexOf('.'));
	}

	private static String read(Path file) {
		try {
			return Files.readString(file);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
