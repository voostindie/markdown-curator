package nl.ulso.curator.query;

import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

/// Defines the configuration of a query in a document; a single query can have many instances; each
/// instance has its own definition.
public interface QueryDefinition
{
    /// @return the name of the [Query] this definition belongs to.
    String queryName();

    /// @return the configuration of this query instance; a dictionary of key-value pairs.
    Dictionary configuration();

    /// @return the document this query is run against.
    Document document();

    /// @return the persisted hash of the output of this query instance.
    String outputHash();
}
