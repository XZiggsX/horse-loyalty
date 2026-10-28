package com.xziggsx.horseloyalty.core.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A titled group of "label: value" rows about one horse, for the dev HUD and {@code /hl info}.
 * Plain English, since it's a developer and operator tool.
 *
 * @param title the section's title, such as "Loyalty"
 * @param rows  the rows, top to bottom
 */
public record DebugSection(String title, List<Row> rows) {
	/**
	 * One row.
	 *
	 * @param label the label, such as "Points"
	 * @param value the value as text
	 * @param tone  how the value reads
	 */
	public record Row(String label, String value, DebugTone tone) {
	}

	/** An empty section to add rows to. */
	public DebugSection(String title) {
		this(title, new ArrayList<>());
	}

	/** Adds a plain row. Returns this section. */
	public DebugSection add(String label, String value) {
		return add(label, value, DebugTone.NORMAL);
	}

	/** Adds a row with a tone. Returns this section. */
	public DebugSection add(String label, String value, DebugTone tone) {
		rows.add(new Row(label, value, tone));
		return this;
	}

	/** The section as plain text lines: the title, then each row indented. */
	public List<String> lines() {
		List<String> lines = new ArrayList<>();
		lines.add(title + ":");
		for (Row row : rows) {
			lines.add("  " + row.label() + ": " + row.value());
		}
		return lines;
	}

	/** Ticks as "45s" or "2m 05s". */
	public static String seconds(long ticks) {
		long seconds = ticks / 20;
		return seconds < 60 ? seconds + "s" : String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
	}
}
