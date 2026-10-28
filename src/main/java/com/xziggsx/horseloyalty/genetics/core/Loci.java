package com.xziggsx.horseloyalty.genetics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every locus a horse has, in a fixed order, and the versions each can hold (the nested enums,
 * listed strongest first where one beats another). Which version wins is up to the traits.
 */
public final class Loci {
	private static final List<Locus<?>> REGISTERED = new ArrayList<>();
	/** Every registered locus, in registration order. */
	public static final List<Locus<?>> ALL = Collections.unmodifiableList(REGISTERED);

	public static final Locus<RedBlack> RED_BLACK = register(new Locus<>("red_black", RedBlack.class));
	public static final Locus<Agouti> AGOUTI = register(new Locus<>("agouti", Agouti.class));
	public static final Locus<Cream> CREAM = register(new Locus<>("cream", Cream.class));
	public static final Locus<Gray> GRAY = register(new Locus<>("gray", Gray.class));
	public static final Locus<Pinto> PINTO = register(new Locus<>("pinto", Pinto.class));
	public static final Locus<Leopard> LEOPARD = register(new Locus<>("leopard", Leopard.class));
	public static final Locus<Socks> SOCKS = register(new Locus<>("socks", Socks.class));
	public static final Locus<Temperament> TEMPERAMENT = register(new Locus<>("temperament", Temperament.class));

	private Loci() {
	}

	/** Adds a locus to {@link #ALL}. Its id must be new. */
	public static synchronized <A extends Enum<A>> Locus<A> register(Locus<A> locus) {
		if (byId(locus.id()) != null) {
			throw new IllegalArgumentException("Duplicate locus " + locus.id());
		}
		REGISTERED.add(locus);
		return locus;
	}

	/** The locus with this id, or null. */
	public static Locus<?> byId(String id) {
		for (Locus<?> locus : REGISTERED) {
			if (locus.id().equals(id)) {
				return locus;
			}
		}
		return null;
	}

	// --- Versions of each locus ------------------------------------------------

	/** Versions of the {@code red_black} locus. Black pigment is dominant; two RED copies make a chestnut base. */
	public enum RedBlack {
		BLACK,
		RED
	}

	/** Versions of the {@code agouti} locus, strongest first: bay beats seal brown, which beats black. */
	public enum Agouti {
		BAY,
		SEAL_BROWN,
		BLACK
	}

	/** Versions of the {@code cream} locus. Works by dosage: one CREAM copy lightens, two lighten more (white on a chestnut base). */
	public enum Cream {
		CREAM,
		NONE
	}

	/** Versions of the {@code gray} locus. One GRAY copy turns any coat gray. */
	public enum Gray {
		GRAY,
		NONE
	}

	/** Versions of the {@code pinto} locus. One PINTO copy gives white patches. */
	public enum Pinto {
		PINTO,
		NONE
	}

	/** Versions of the {@code leopard} locus. Works by dosage: one LEOPARD copy gives white dots, two give black dots. */
	public enum Leopard {
		LEOPARD,
		NONE
	}

	/** Versions of the {@code socks} locus. SOCKS is recessive: only two copies give white legs. */
	public enum Socks {
		NONE,
		SOCKS
	}

	/**
	 * Versions of the {@code temperament} locus. Any quirk beats STEADY. A horse with two different
	 * quirks shows one of them, picked 50/50 once at birth.
	 */
	public enum Temperament {
		BRAVE,
		SKITTISH,
		CURIOUS,
		LAZY,
		STEADY
	}
}
