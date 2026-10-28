package com.xziggsx.horseloyalty.genetics.view;

import com.xziggsx.horseloyalty.genetics.core.Trait;

/**
 * A trait this kind of horse shows, with what one horse shows now.
 *
 * @param trait   the trait
 * @param current what the horse shows, or null if it has no value yet
 */
public record ShownTrait<T>(Trait<T> trait, T current) {
}
