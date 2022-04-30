package nl.ulso.macu.vault;

import java.util.List;
import java.util.Objects;

import static java.lang.Character.isAlphabetic;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static nl.ulso.macu.vault.Dictionary.yamlDictionary;

/**
 * Represents a query in a Markdown document. A query consists of 3 parts: the name, the
 * configuration and the result.
 * <p/>
 * Queries do not exist in any Markdown specification, which is why they're encoded as HTML
 * comments. This also ensures that the query definitions don't show up when rendering the
 * Markdown to HTML; only the result does.
 * <p/>
 * The informal BNF specification for queries is:
 * <pre>
 *     query ::= "&lt;!--query" (":" &lt;name>) (&lt;configuration>) "-->" &lt;newline>
 *               &lt;output> &lt;newline>
 *               "&lt;!--/query-->"
 *     name ::= string of alphabetical characters
 *     configuration ::= YAML
 *     output ::= arbitrary string
 * </pre>
 * This format is processed by this tool; it's why this tool exists in this first place. It picks
 * up the {@code name} and {@code configuration}, interprets it, runs it, and writes the results
 * in {@code output}.
 * <p/>
 * If no name is provided in the content, the default {@value DEFAULT_NAME} is assumed.
 * <p/>
 * The {@code configuration} is a YAML map. It can be omitted if the query needs no
 * configuration.
 * <p/>
 * The simplest way to add a new query to a page is to add an empty query block. After saving the
 * page, this tool will pick it up and insert the output, which consists of a list of available
 * queries. Then go from there.
 */
public final class QueryBlock
        extends LineContainer
        implements Fragment
{
    static final String QUERY_CONFIGURATION_PREFIX = "<!--query";
    private static final String QUERY_CONFIGURATION_POSTFIX = "-->";
    static final String QUERY_OUTPUT_PREFIX = "<!--/query";
    static final String QUERY_OUTPUT_POSTFIX = "-->";
    private static final char QUERY_NAME_MARKER = ':';
    private static final String DEFAULT_NAME = "none";

    private final String name;
    private final Dictionary configuration;
    private final String result;
    private final int resultStartIndex;
    private final int resultEndIndex;

    QueryBlock(List<String> lines, int documentLineIndex)
    {
        super(lines);
        var parser = new QueryParser(lines);
        name = parser.name();
        configuration = yamlDictionary(parser.configuration());
        result = parser.result();
        resultStartIndex = documentLineIndex + parser.resultStartIndex() + 1;
        resultEndIndex = documentLineIndex + lines.size() - 1;
    }

    public String name()
    {
        return name;
    }

    public Dictionary configuration()
    {
        return configuration;
    }

    public String result()
    {
        return result;
    }

    /**
     * @return The index of the line within the document that marks the start of the query result.
     */
    public int resultStartIndex()
    {
        return resultStartIndex;
    }

    /**
     * @return The index of the line  within the document that marks the end of the query result.
     */
    public int resultEndIndex()
    {
        return resultEndIndex;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof QueryBlock queryBlock)
        {
            return Objects.equals(name, queryBlock.name)
                    && Objects.equals(configuration, queryBlock.configuration)
                    && Objects.equals(result, queryBlock.result)
                    && Objects.equals(resultStartIndex, queryBlock.resultStartIndex)
                    && Objects.equals(resultEndIndex, queryBlock.resultEndIndex);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, configuration, result, resultStartIndex, resultEndIndex);
    }

    @Override
    public boolean isEmpty()
    {
        return configuration.isEmpty() && result.isEmpty();
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    private static final class QueryParser
    {
        private String name;
        private String configuration;
        private String result;
        private int resultStartIndex;

        QueryParser(List<String> lines)
        {
            var query = join(lineSeparator(), lines.subList(0, lines.size() - 1))
                    .substring(QUERY_CONFIGURATION_PREFIX.length())
                    .trim();
            if (query.length() > 0 && query.charAt(0) == QUERY_NAME_MARKER)
            {
                var end = 1;
                while (isAlphabetic(query.charAt(end)))
                {
                    end++;
                }
                if (end > 1)
                {
                    this.name = query.substring(1, end).toLowerCase();
                }
                query = query.substring(end);
            }
            int split = query.indexOf(QUERY_CONFIGURATION_POSTFIX);
            if (split != -1)
            {
                configuration = query.substring(0, split).trim();
                result = query.substring(split + QUERY_CONFIGURATION_POSTFIX.length()).trim();
                while (!lines.get(resultStartIndex).endsWith(QUERY_OUTPUT_POSTFIX))
                {
                    resultStartIndex++;
                }
            }
        }

        String name()
        {
            return name != null ? name : DEFAULT_NAME;
        }

        String configuration()
        {
            return configuration != null ? configuration : "";
        }

        String result()
        {
            return result != null ? result : "";
        }

        int resultStartIndex()
        {
            return resultStartIndex;
        }
    }
}
