package com.xziggsx.horseloyalty.genetics.api;

import com.xziggsx.horseloyalty.genetics.core.GenePair;

/**
 * Whether each copy at a locus shows on a horse, for display: a copy that shows helps decide
 * what the horse looks like, one that doesn't is only carried (it can still show in foals).
 *
 * @param firstShows  whether the first copy shows
 * @param secondShows whether the second copy shows
 */
public record CopyRole(boolean firstShows, boolean secondShows) {
	public static final CopyRole NEITHER = new CopyRole(false, false);
	public static final CopyRole BOTH = new CopyRole(true, true);

	/** The copies that are this version show; the others don't. */
	public static <A extends Enum<A>> CopyRole where(GenePair<A> pair, A version) {
		return new CopyRole(pair.first() == version, pair.second() == version);
	}
}
