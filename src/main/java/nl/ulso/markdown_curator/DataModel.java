package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;

/**
 * Represents a data model that is derived from a {@link nl.ulso.markdown_curator.vault.Vault}.
 * <p/>
 * Whenever a change to the vault is detected, the data model gets refreshed.
 * <p/>
 * Data models are particularly useful to base queries on, especially when multiple queries require
 * the same data model, or when multiple instances of the same query do. For example, imagine a
 * journal data model that keeps a timeline of all activities across all documents in the vault,
 * and queries on top of that model that select specific parts of the journal.
 * <p/>
 * <b>Important</b>: data models must be singletons (e.g. marked with
 * {@link jakarta.inject.Singleton})! They are accessed by queries running in parallel and must
 * therefore be thread-safe. The {@link Curator} ensures that writes are synchronized, and that
 * no reads can happen while writes are in progress. This of course only holds if in your
 * implementations you don't update any internal data structures on <em>read</em>.
 */
public interface DataModel
{
    int ORDER_FIRST = 0;
    int ORDER_LAST = Integer.MAX_VALUE;

    void vaultChanged(VaultChangedEvent event);

    /**
     * If your model depends on other models, make sure you return a higher order here.
     * If your model doesn't depend on any other model, use {@link #ORDER_FIRST}; this is the
     * default.
     *
     * @return This model's order, used to sort all data models when refreshing them;
     */
    default int order()
    {
        return ORDER_FIRST;
    }
}
