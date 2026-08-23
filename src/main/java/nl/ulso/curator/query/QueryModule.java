package nl.ulso.curator.query;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.query.builtin.*;

@Module
public abstract class QueryModule
{
    @Binds
    abstract QueryCatalog bindQueryCatalog(DefaultQueryCatalog queryCatalog);

    @Binds
    abstract QueryResultFormatterRegistry bindQueryResultFormatterRegistry(
        DefaultQueryResultFormatterRegistry registry);

    @Binds
    abstract QueryResultFactory bindQueryResultFactory(
        DefaultQueryResultFactory queryResultFactory);

    @Binds
    abstract GeneralMessages bindGeneralMessages(ResourceBundleGeneralMessages messages);

    @Binds
    @IntoSet
    abstract Query bindListQuery(ListQuery listQuery);

    @Binds
    @IntoSet
    abstract Query bindTableQuery(TableQuery tableQuery);

    @Binds
    @IntoSet
    abstract Query bindTableOfContentsQuery(TableOfContentsQuery tableOfContentsQuery);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> stringMarkdownFormatter(StringMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> errorMarkdownFormatter(ErrorMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> tableMarkdownFormatter(TableMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> unorderedListMarkdownFormatter(
        UnorderedListMarkdownFormatter formatter);
}
