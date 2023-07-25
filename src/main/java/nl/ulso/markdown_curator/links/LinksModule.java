package nl.ulso.markdown_curator.links;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import nl.ulso.markdown_curator.DataModel;
import nl.ulso.markdown_curator.query.Query;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

/**
 * Module that adds the {@link LinksModel} and one query on top of it: {@link DeadLinksQuery}.
 * <p/>
 * To use this module, install it in your own curator.
 */
public class LinksModule
        extends AbstractModule
{
    @Override
    protected void configure()
    {
        newSetBinder(binder(), DataModel.class).addBinding().to(LinksModel.class);
        Multibinder<Query> queryBinder = newSetBinder(binder(), Query.class);
        queryBinder.addBinding().to(DeadLinksQuery.class);
    }
}
