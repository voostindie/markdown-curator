package nl.ulso.curator.main;

import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionLifecycle;
import nl.ulso.curator.extension.ServiceRegistration;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.query.QueryResultFormatter;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.HashSet;
import java.util.Set;

import static nl.ulso.curator.extension.ServiceRegistration.service;

final class ExtensionStub
    implements Extension
{
    private final Set<ChangeProcessor> changeProcessors = new HashSet<>();
    private final Set<Query> queries = new HashSet<>();
    private final Set<QueryResultFormatter<?>> formatters = new HashSet<>();
    private final Set<ServiceRegistration> services = new HashSet<>();
    private final Set<ExtensionLifecycle> lifecycles = new HashSet<>();

    static ExtensionStub extension(ChangeProcessor... processors)
    {
        var extension = new ExtensionStub();
        extension.changeProcessors.addAll(Set.of(processors));
        return extension;
    }

    ExtensionStub withQueries(Query... queries)
    {
        this.queries.addAll(Set.of(queries));
        return this;
    }

    ExtensionStub withFormatters(QueryResultFormatter<?>... formatters)
    {
        this.formatters.addAll(Set.of(formatters));
        return this;
    }

    <T> ExtensionStub withService(Class<T> type, T instance)
    {
        services.add(service(type, instance));
        return this;
    }

    ExtensionStub withLifecycle(ExtensionLifecycle lifecycle)
    {
        lifecycles.add(lifecycle);
        return this;
    }

    @Override
    public Set<ChangeProcessor> changeProcessors()
    {
        return changeProcessors;
    }

    @Override
    public Set<Query> queries()
    {
        return queries;
    }

    @Override
    public Set<QueryResultFormatter<?>> queryResultFormatters()
    {
        return formatters;
    }

    @Override
    public Set<MeasurementTracker> measurementTrackers()
    {
        return Set.of();
    }

    @Override
    public Set<ServiceRegistration> services()
    {
        return services;
    }

    @Override
    public Set<ExtensionLifecycle> lifecycles()
    {
        return lifecycles;
    }
}
