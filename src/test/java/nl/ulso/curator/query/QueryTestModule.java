package nl.ulso.curator.query;

import java.util.Locale;

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
}
