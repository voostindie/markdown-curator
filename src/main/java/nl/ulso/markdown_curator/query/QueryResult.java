package nl.ulso.markdown_curator.query;

/**
 * Represents the result of running a query.
 */
@FunctionalInterface
public interface QueryResult
{
    /**
     * @return A Markdown representation of the result.
     */
    String toMarkdown();
}
