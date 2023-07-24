package nl.ulso.markdown_curator.vault;

import nl.ulso.markdown_curator.query.QueryDefinition;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.vault.Dictionary.yamlDictionary;

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
 *     query ::= "&lt;!--query:" &lt;name> (&lt;configuration>)? "-->" &lt;newline>
 *               &lt;output> &lt;newline>
 *               "&lt;!--/query" (" (" &lt;hash> ")")? "-->"
 *     name ::= string of alphabetical characters
 *     configuration ::= YAML map
 *     output ::= arbitrary string
 *     hash ::= hash of the output
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
 * The {@code hash} is hash computed of the output by this tool, used to check whether the output
 * of a query has changed.
 * <p/>
 * The simplest way to add a new query to a page is to add an empty query block. After saving the
 * page, this tool will pick it up and insert the output, which consists of a list of available
 * queries. Then go from there.
 */
public final class QueryBlock
        extends LineContainer
        implements Fragment, QueryDefinition
{
    static final String QUERY_CONFIGURATION_PREFIX = "<!--query";
    private static final String QUERY_CONFIGURATION_POSTFIX = "-->";
    static final String QUERY_OUTPUT_PREFIX = "<!--/query";
    private static final String QUERY_HASH_PREFIX = "(";
    private static final String QUERY_HASH_POSTFIX = ")";
    static final String QUERY_OUTPUT_POSTFIX = "-->";
    private static final char QUERY_NAME_MARKER = ':';
    private static final String DEFAULT_NAME = "none";

    private final String queryName;
    private final Dictionary configuration;
    private final String outputHash;
    private final int resultStartIndex;
    private final int resultEndIndex;

    QueryBlock(List<String> lines, int documentLineIndex)
    {
        super(emptyList());
        var parser = new QueryParser(lines);
        queryName = parser.queryName();
        configuration = yamlDictionary(parser.configuration());
        outputHash = parser.outputHash();
        resultStartIndex = documentLineIndex + parser.resultStartIndex() + 1;
        resultEndIndex = documentLineIndex + lines.size() - 1;
    }

    public String queryName()
    {
        return queryName;
    }

    public Dictionary configuration()
    {
        return configuration;
    }

    public String outputHash()
    {
        return outputHash;
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
            return Objects.equals(queryName, queryBlock.queryName)
                    && Objects.equals(configuration, queryBlock.configuration)
                    && Objects.equals(outputHash, queryBlock.outputHash)
                    && Objects.equals(resultStartIndex, queryBlock.resultStartIndex)
                    && Objects.equals(resultEndIndex, queryBlock.resultEndIndex);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(queryName, configuration, outputHash, resultStartIndex, resultEndIndex);
    }

    @Override
    public List<String> lines()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String content()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty()
    {
        return configuration.isEmpty() && outputHash.isEmpty();
    }

    @Override

    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    public static void writeQueryEndWithHash(PrintWriter writer, String hash)
    {
        writer.print(QUERY_OUTPUT_PREFIX);
        writer.print(" ");
        writer.print(QUERY_HASH_PREFIX);
        writer.print(hash);
        writer.print(QUERY_HASH_POSTFIX);
        writer.println(QUERY_OUTPUT_POSTFIX);
    }

    private static final class QueryParser
    {
        private String queryName;
        private String configuration;
        private String outputHash;
        private int resultStartIndex;

        QueryParser(List<String> lines)
        {
            var query = join(lineSeparator(), lines.subList(0, lines.size() - 1))
                    .substring(QUERY_CONFIGURATION_PREFIX.length())
                    .trim();
            if (query.length() > 0 && query.charAt(0) == QUERY_NAME_MARKER)
            {
                var end = 1;
                while (true)
                {
                    char c = query.charAt(end);
                    if (!isLetterOrDigit(c))
                    {
                        break;
                    }
                    end++;
                }
                if (end > 1)
                {
                    queryName = query.substring(1, end).toLowerCase();
                }
                query = query.substring(end);
            }
            int split = query.indexOf(QUERY_CONFIGURATION_POSTFIX);
            if (split != -1)
            {
                configuration = query.substring(0, split).trim();
                while (!lines.get(resultStartIndex).endsWith(QUERY_OUTPUT_POSTFIX))
                {
                    resultStartIndex++;
                }
            }
            var line = lines.get(lines.size() - 1);
            int hashStart = line.indexOf(QUERY_HASH_PREFIX, QUERY_OUTPUT_PREFIX.length());
            if (hashStart != -1)
            {
                int hashEnd = line.indexOf(QUERY_HASH_POSTFIX, hashStart);
                if (hashEnd != -1)
                {
                    outputHash = line.substring(hashStart + 1, hashEnd);
                }
            }
        }

        String queryName()
        {
            return queryName != null ? queryName : DEFAULT_NAME;
        }

        String configuration()
        {
            return configuration != null ? configuration : "";
        }

        String outputHash() { return outputHash != null ? outputHash : ""; }

        int resultStartIndex()
        {
            return resultStartIndex;
        }
    }
}
