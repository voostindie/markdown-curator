package nl.ulso.curator.addon.links;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;

/// Module that adds the [LinksModel] and one query on top of it: [DeadLinksQuery].
///
/// To use this module, install it in your own curator.
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
