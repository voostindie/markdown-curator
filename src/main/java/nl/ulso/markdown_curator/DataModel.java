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
 * Important: data models are accessed by queries running in parallel and must therefore be
 * thread-safe. Note however that each refresh is guaranteed to be synchronized.
 */
public interface DataModel
{
    void vaultChanged(VaultChangedEvent event);
}
