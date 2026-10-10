package nl.ulso.curator.query;

import java.util.Locale;
import java.util.Set;

import static java.util.Collections.emptySet;

public class QueryTestModule
{
    public static QueryCatalog createEmptyCatalog()
    {
        return createQueryCatalog(emptySet());
    }

    public static QueryCatalog createQueryCatalog(Set<Query> queries)
    {
        return new DefaultQueryCatalog(queries, new DefaultQueryResultFactory());
    }

    public static QueryResultFactory createQueryResultFactory()
    {
        return new DefaultQueryResultFactory();
    }

    public static GeneralMessages createMessages(Locale locale)
    {
        return new ResourceBundleGeneralMessages(locale);
    }

    public static QueryResultFormatterRegistry createQueryResultFormatterRegistry()
    {
        return createQueryResultFormatterRegistry(Set.of(
            new ErrorMarkdownFormatter(),
            new ErrorJsonFormatter(),
            new StringMarkdownFormatter(),
            new StringJsonFormatter(),
            new TableMarkdownFormatter(),
            new TableJsonFormatter(),
            new UnorderedListMarkdownFormatter(),
            new UnorderedListJsonFormatter(),
            new FolderResultJsonFormatter()
        ));
    }

    public static QueryResultFormatterRegistry createQueryResultFormatterRegistry(
        Set<QueryResultFormatter<?>> formatters)
    {
        return new DefaultQueryResultFormatterRegistry(formatters);
    }
}
