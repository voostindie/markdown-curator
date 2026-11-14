package nl.ulso.curator.extension;

import dagger.Module;
import dagger.multibindings.Multibinds;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.query.QueryResultFormatter;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.Set;

/// Declares the (possibly empty) sets that every [Extension] exposes. Include this module in every
/// extension subcomponent.
// S1452: Dagger multibindings requires QueryResultFormatter<?> to bind Set<QueryResultFormatter<?>>
@SuppressWarnings("java:S1452")
@Module
public abstract class ExtensionModule
{
    @Multibinds
    @ExtensionBinding
    abstract Set<ChangeProcessor> bindExtensionChangeProcessors();

    @Multibinds
    @ExtensionBinding
    abstract Set<Query> bindExtensionQueries();

    @Multibinds
    @ExtensionBinding
    abstract Set<QueryResultFormatter<?>> bindExtensionQueryResultFormatters();

    @Multibinds
    @ExtensionBinding
    abstract Set<MeasurementTracker> bindExtensionMeasurementTrackers();

    @Multibinds
    @ExtensionBinding
    abstract Set<ServiceRegistration> bindExtensionServices();

    @Multibinds
    @ExtensionBinding
    abstract Set<ExtensionLifecycle> bindExtensionLifecycles();
}
