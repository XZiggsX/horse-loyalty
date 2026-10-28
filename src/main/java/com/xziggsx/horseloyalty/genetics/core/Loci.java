package com.xziggsx.horseloyalty.genetics.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Every locus a horse has, in a fixed order. */
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
}
