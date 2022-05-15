package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.vault.QueryBlock;

import java.util.Map;

/**
 * Represents a single query that can be executed against a {@link QueryBlock}; the configuration
 * of the query comes from the block.
 */
public interface Query
{
    /**
     * @return The name of the query.
     */
    String name();

    /**
     * @return A short, one-line description of the query, in Markdown.
     */
    String description();

    /**
     * @return Map of name-description pairs of configuration values; each description is a
     * Markdown one-liner.
     */
    Map<String, String> supportedConfiguration();

    /**
     * Runs the query against and produces result.
     *
     * @param definition definition to run this query against.
     * @return Result of running the query against the block.
     */
    QueryResult run(QueryDefinition definition);
}
