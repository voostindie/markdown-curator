package nl.ulso.curator.query;

import java.util.Locale;
import java.util.Set;

import static java.util.Collections.emptySet;

public class QueryTestModule
{
    public static QueryCatalog createEmptyCatalog()
    {
        return new DefaultQueryCatalog(emptySet(), new DefaultQueryResultFactory());
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
        var formatters = Set.of(
            new ErrorMarkdownFormatter(),
            new StringMarkdownFormatter(),
            new TableMarkdownFormatter(),
            new UnorderedListMarkdownFormatter()
        );
        return new DefaultQueryResultFormatterRegistry(formatters);
    }
}
