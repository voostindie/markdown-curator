package nl.ulso.markdown_curator.links;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.markdown_curator.ChangeProcessor;
import nl.ulso.markdown_curator.query.Query;

/**
 * Module that adds the {@link LinksModel} and one query on top of it: {@link DeadLinksQuery}.
 * <p/>
 * To use this module, install it in your own curator.
 */
@Module
public abstract class LinksModule
{
    @Binds
    @IntoSet
    abstract ChangeProcessor bindDataModel(LinksModel linksModel);

    @Binds
    @IntoSet
    abstract Query bindDeadLinksQuery(DeadLinksQuery deadLinksQuery);
}
