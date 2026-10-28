package com.xziggsx.horseloyalty.core.link;

/**
 * A typed handle to something one feature provides to others, with the answer to give while it's
 * off. The key and its interface live in the providing feature's {@code api} package; core only
 * knows this generic record, so a new link never touches core.
 *
 * @param id      a unique name such as "loyalty:bond", for logs
 * @param type    the interface the provider implements
 * @param neutral the stand-in that answers while the providing feature is off
 * @param <T>     the interface
 */
public record LinkKey<T>(String id, Class<T> type, T neutral) {
}
