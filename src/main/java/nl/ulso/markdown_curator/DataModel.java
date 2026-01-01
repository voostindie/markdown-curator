package nl.ulso.markdown_curator;

import java.util.Set;

import static java.util.Collections.emptySet;

/// Represents a data model derived from a [nl.ulso.markdown_curator.vault.Vault]; whenever a change
/// to the vault is detected, the data model gets refreshed.
///
/// Data models are particularly useful to base queries on, especially when multiple queries require
/// the same data model or when multiple instances of the same query do. For example, imagine a
/// journal data model that keeps a timeline of all activities across all documents in the vault,
/// and queries on top of that model that select specific parts of the journal.
///
/// **Important**: data models must be singletons (e.g. marked with [jakarta.inject.Singleton]) and
/// thread-safe! The [Curator] ensures that writes are synchronized, and that no reads can happen
/// while writes are in progress. Reads happen in parallel because all queries are executed
/// concurrently.
public interface DataModel
{
    /// Process a changelog on this data model.
    /// @param changelog changelog to process.
    /// @return changelog with changes in this data model, if any.
    Changelog process(Changelog changelog);

    /// Returns the set of data models that this model depends on.
    ///
    /// This method is called only once, during application startup, to order the data models in
    /// such a way that dependent models are refreshed before models that depend on them.
    ///
    /// @return the set of data models this data model depends on.
    default Set<DataModel> dependentModels()
    {
        return emptySet();
    }
}
