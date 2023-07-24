package nl.ulso.markdown_curator.vault;

import nl.ulso.markdown_curator.query.QueryDefinition;

import java.util.List;
import java.util.Objects;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
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
        extends FragmentBase
        implements Fragment, QueryDefinition
{
    static final String QUERY_CONFIGURATION_PREFIX = "<!--query";
    private static final String QUERY_CONFIGURATION_POSTFIX = "-->";
    public static final String QUERY_OUTPUT_PREFIX = "<!--/query";
    public static final String QUERY_HASH_PREFIX = "(";
    public static final String QUERY_HASH_POSTFIX = ")";
    public static final String QUERY_OUTPUT_POSTFIX = "-->";
    private static final char QUERY_NAME_MARKER = ':';
    private static final String DEFAULT_NAME = "none";

    private final String definitionString;
    private final String queryName;
    private final Dictionary configuration;
    private final String outputHash;

    QueryBlock(List<String> lines)
    {
        var definitionEnd = findDefinitionEnd(lines);
        definitionString = join(lineSeparator(), lines.subList(0, definitionEnd + 1));
        var parser = new QueryParser(lines, definitionEnd);
        queryName = parser.queryName();
        configuration = yamlDictionary(parser.configuration());
        outputHash = parser.outputHash();
    }

    private int findDefinitionEnd(List<String> lines)
    {
        var i = 0;
        for (var line : lines)
        {
            if (line.endsWith(QUERY_CONFIGURATION_POSTFIX))
            {
                return i;
            }
            i++;
        }
        throw new IllegalStateException("A query MUST have a definition ending");
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
                   && Objects.equals(definitionString, queryBlock.definitionString)
                   && Objects.equals(configuration, queryBlock.configuration)
                   && Objects.equals(outputHash, queryBlock.outputHash);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(queryName, definitionString, configuration, outputHash);
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    public String markdown(String queryResultOutput, String queryResultHash)
    {
        return definitionString + lineSeparator() +
               queryResultOutput + lineSeparator() +
               QUERY_OUTPUT_PREFIX + " " +
               QUERY_HASH_PREFIX + queryResultHash + QUERY_HASH_POSTFIX +
               QUERY_OUTPUT_POSTFIX + lineSeparator();
    }

    private static final class QueryParser
    {
        private String queryName;
        private String configuration;
        private String outputHash;

        QueryParser(List<String> lines, int definitionEnd)
        {
            var query = join(lineSeparator(), lines.subList(0, definitionEnd + 1))
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
            }
            var lastLine = lines.get(lines.size() - 1);
            int hashStart = lastLine.indexOf(QUERY_HASH_PREFIX, QUERY_OUTPUT_PREFIX.length());
            if (hashStart != -1)
            {
                int hashEnd = lastLine.indexOf(QUERY_HASH_POSTFIX, hashStart);
                if (hashEnd != -1)
                {
                    outputHash = lastLine.substring(hashStart + 1, hashEnd);
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

        String outputHash() {return outputHash != null ? outputHash : "";}
    }
}
