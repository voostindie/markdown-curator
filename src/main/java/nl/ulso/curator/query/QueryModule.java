package nl.ulso.curator.query;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.query.builtin.*;

@Module
public abstract class QueryModule
{
    @Binds
    abstract QueryCatalog bindQueryCatalog(QueryCatalogImpl queryCatalog);

    @Binds
    abstract QueryResultFactory bindQueryResultFactory(QueryResultFactoryImpl queryResultFactory);

    @Binds
    abstract GeneralMessages bindGeneralMessages(ResourceBundledGeneralMessages generalMessages);

    @Binds
    @IntoSet
    abstract Query bindListQuery(ListQuery listQuery);

    @Binds
    @IntoSet
    abstract Query bindTableQuery(TableQuery tableQuery);

    @Binds
    @IntoSet
    abstract Query bindTableOfContentsQuery(TableOfContentsQuery tableOfContentsQuery);
}
