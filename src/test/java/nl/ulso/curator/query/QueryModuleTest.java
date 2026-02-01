package nl.ulso.curator.query;

import java.util.Locale;

import static java.util.Collections.emptySet;

public class QueryModuleTest
{
    public static QueryCatalog createEmptyCatalog()
    {
        return new QueryCatalogImpl(emptySet(), new QueryResultFactoryImpl());
    }

    public static QueryResultFactory createQueryResultFactory()
    {
        return new QueryResultFactoryImpl();
    }

    public static GeneralMessages createMessages(Locale locale)
    {
        return new ResourceBundledGeneralMessages(locale);
    }
}
