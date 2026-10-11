package nl.ulso.curator.query;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;

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
    abstract Query bindListFolderContentQuery(ListFolderContentQuery query);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindEmptyMarkdownFormatter(EmptyMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindEmptyJsonFormatter(EmptyJsonFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindStringMarkdownFormatter(StringMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindStringJsonFormatter(StringJsonFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindErrorMarkdownFormatter(ErrorMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindErrorJsonFormatter(ErrorJsonFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindTableMarkdownFormatter(TableMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindTableJsonFormatter(TableJsonFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindUnorderedListMarkdownFormatter(
        UnorderedListMarkdownFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindUnorderedListJsonFormatter(
        UnorderedListJsonFormatter formatter);

    @Binds
    @IntoSet
    abstract QueryResultFormatter<?> bindFolderResultJsonFormatter(
        FolderResultJsonFormatter formatter);
}
