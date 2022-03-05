package nl.ulso.obsidian.watcher.vault;

import java.util.List;
import java.util.Objects;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

/**
 * Represents a query in a Markdown document. A query consists of 3 parts: the definition,
 * the (optional) result and the end.
 * <p/>
 * Queries do not exist in any Markdown specification, which is why they're encoded as HTML
 * comments. This also ensures that the query definitions don't show up when rendering the
 * Markdown to HTML; just the output.
 * <p/>
 * Queries look as follows:
 * <pre>
 *     &lt;!--query DEFINITION-->
 *     OUTPUT
 *     &lt;!--/query-->
 * </pre>
 * This format is processed by this tool; it's why this tool exists in this first place. It
 * picks up the {@code DEFINITION}, interprets it, runs it, and writes the results in
 * {@code OUTPUT}.
 */
public final class Query
        extends LineContainer
        implements Fragment
{
    private static final int QUERY_PREFIX_LENGTH = MarkdownTokenizer.QUERY_BEGIN_MARKER.length();
    private static final int QUERY_POSTFIX_LENGTH = MarkdownTokenizer.QUERY_CLOSING.length();

    private final String definition;
    private final String result;

    Query(List<String> lines, int resultOffset)
    {
        super(lines);
        this.definition = extractDefinition(lines, resultOffset);
        this.result = extractResult(lines, resultOffset);
    }

    private String extractDefinition(List<String> lines, int resultOffset)
    {
        var definition = join(lineSeparator(), lines.subList(0, resultOffset))
                .substring(QUERY_PREFIX_LENGTH).trim();
        return definition.substring(0, definition.length() - QUERY_POSTFIX_LENGTH).trim();
    }

    private String extractResult(List<String> lines, int resultOffset)
    {
        return join(lineSeparator(), lines.subList(resultOffset, lines.size() - 1)).trim();
    }

    public String definition()
    {
        return definition;
    }

    public String result()
    {
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Query query)
        {
            return Objects.equals(definition, query.definition)
                    && Objects.equals(result, query.result);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(definition, result);
    }

    @Override
    public boolean isEmpty()
    {
        return definition.isEmpty() && result.isEmpty();
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }
}
