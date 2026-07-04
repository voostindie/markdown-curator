package nl.ulso.curator.query;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class DaggerQueryTestModule
{
    @Binds
    abstract QueryResultFactory bindQueryResultFactory(DefaultQueryResultFactory factory);

    @Binds
    abstract GeneralMessages bindMessages(ResourceBundleGeneralMessages messages);
}
