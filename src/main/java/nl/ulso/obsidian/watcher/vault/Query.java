package nl.ulso.obsidian.watcher.vault;

import java.util.List;
import java.util.Objects;

import static java.lang.Character.isAlphabetic;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static nl.ulso.obsidian.watcher.vault.Dictionary.yamlDictionary;

/**
 * Represents a query in a Markdown document. A query consists of 3 parts: the type, the
 * configuration and the result. The type and configuration together form the query definition.
 * <p/>
 * Queries do not exist in any Markdown specification, which is why they're encoded as HTML
 * comments. This also ensures that the query definitions don't show up when rendering the
 * Markdown to HTML; only the result does.
 * <p/>
 * The informal BNF specification for queries is:
 * <pre>
 *     query ::= "&lt;!--query" (":" &lt;type>) (&lt;configuration>) "-->" &lt;newline>
 *               &lt;output> &lt;newline>
 *               "&lt;!--/query-->"
 *     type ::= string of alphabetical characters
 *     configuration ::= YAML
 *     output ::= arbitrary string
 * </pre>
 * This format is processed by this tool; it's why this tool exists in this first place. It picks
 * up the {@code type} and {@code configuration}, interprets it, runs it, and writes the results
 * in {@code output}.
 * <p/>
 * If no type is provided in the content, the default {@value DEFAULT_TYPE} is assumed.
 * <p/>
 * The {@code configuration} is a YAML map. It can be omitted if the query needs no
 * configuration.
 * <p/>
 * The simplest way to add a new query to a page is to add an empty query block. After saving the
 * page, this tool will pick it up and insert the output, which consists of verbose documentation
 * if the query coulnd't be understood in one way or another.
 */
public final class Query
        extends LineContainer
        implements Fragment
{
    static final String QUERY_CONFIGURATION_PREFIX = "<!--query";
    private static final String QUERY_CONFIGURATION_POSTFIX = "-->";
    static final String QUERY_OUTPUT_PREFIX = "<!--/query";
    static final String QUERY_OUTPUT_POSTFIX = "-->";
    private static final char QUERY_TYPE_MARKER = ':';
    private static final String DEFAULT_TYPE = "cypher";

    private final String type;
    private final Dictionary configuration;
    private final String result;

    Query(List<String> lines)
    {
        super(lines);
        var parser = new QueryParser(lines);
        type = parser.type();
        configuration = yamlDictionary(parser.configuration());
        result = parser.result();
    }

    public String type() {return type;}

    public Dictionary configuration()
    {
        return configuration;
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
            return Objects.equals(type, query.type)
                    && Objects.equals(configuration, query.configuration)
                    && Objects.equals(result, query.result);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, configuration, result);
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
        private String type;
        private String configuration;
        private String result;

        QueryParser(List<String> lines)
        {
            var query = join(lineSeparator(), lines.subList(0, lines.size() - 1))
                    .substring(QUERY_CONFIGURATION_PREFIX.length())
                    .trim();
            if (query.length() > 0 && query.charAt(0) == QUERY_TYPE_MARKER)
            {
                var end = 1;
                while (isAlphabetic(query.charAt(end)))
                {
                    end++;
                }
                if (end > 1)
                {
                    this.type = query.substring(1, end);
                }
                query = query.substring(end);
            }
            int split = query.indexOf(QUERY_CONFIGURATION_POSTFIX);
            if (split != -1)
            {
                configuration = query.substring(0, split).trim();
                result = query.substring(split + QUERY_CONFIGURATION_POSTFIX.length()).trim();
            }
        }

        String type()
        {
            return type != null ? type : DEFAULT_TYPE;
        }

        String configuration()
        {
            return configuration != null ? configuration : "";
        }

        String result()
        {
            return result != null ? result : "";
        }
    }
}
