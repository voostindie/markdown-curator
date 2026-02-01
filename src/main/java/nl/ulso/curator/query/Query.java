package nl.ulso.curator.query;

import nl.ulso.curator.changelog.Changelog;

import java.util.Map;

/// Represents a single query that can be executed against a definition; the configuration of the
/// query comes from the definition.
///
/// Important:
///
/// - Queries are run in parallel and must therefore be thread-safe.
/// - For the same input, the query must consistently generate the same output. The curator
/// internally uses hashes computed from the query output to determine whether a document must be
/// updated with new output. Make sure there is no randomness in the output. Order all lists, for
/// example.
public interface Query
{
    /// @return The name of the query.
    String name();

    /// @return A short, one-line description of the query, in Markdown. This description is
    /// included in the output of unknown queries.
    String description();

    /// @return Map of name-description pairs of configuration values; each description is a
    /// Markdown one-liner. This description is included in the output of the `help` query.
    Map<String, String> supportedConfiguration();

    /// Returns whether the changelog combined with the query definition might generate new output;
    /// when unsure, return `true`.
    ///
    /// Returning `true` does not mean that the query output _must_ be different from what is
    /// persisted in the document, nor does it mean that the document with this query definition
    /// will be updated. Returning `true` _does_ guarantee that the query will be executed. The
    /// output hash will then be compared with the persisted hash. If it is the same and if there
    /// are no other changes for the same document, in the end nothing will happen; the document
    /// will not be rewritten to disk.
    ///
    /// Returning `false` does not mean that the query will _not_ be executed. If the document
    /// containing the query definition does not contain an output hash for the query, or if there
    /// is another query in the document that requires the document to be rewritten, then the query
    /// will be executed anyway.
    ///
    /// Long story short: return `false` if you're 100% sure the query is not impacted. Otherwise,
    /// return `true`.
    ///
    /// @param changelog  Changelog to verify.
    /// @param definition Definition ("instance") of the query to verify.
    /// @return Whether this query is impacted by the changelog.
    boolean isImpactedBy(Changelog changelog, QueryDefinition definition);

    /// Runs the query against and produces a result.
    ///
    /// @param definition definition to run this query against.
    /// @return Result of running the query against the definition.
    QueryResult run(QueryDefinition definition);
}
