package nl.ulso.curator.main;

import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.Set;

record ExtensionStub(Set<ChangeProcessor> changeProcessors, Set<Query> queries)
    implements Extension
{
    static ExtensionStub extension(ChangeProcessor... processors)
    {
        return new ExtensionStub(Set.of(processors), Set.of());
    }

    ExtensionStub withQueries(Query... queries)
    {
        return new ExtensionStub(changeProcessors, Set.of(queries));
    }

    @Override
    public Set<MeasurementTracker> measurementTrackers()
    {
        return Set.of();
    }
}
