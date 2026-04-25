package nl.ulso.curator.statistics;

/// [MeasurementCollector] for use in tests.
public class MeasurementCollectorStub
    implements MeasurementCollector
{
    private final DefaultMeasurementCollector collector = new DefaultMeasurementCollector();

    @Override
    public MeasurementCollector total(Class<?> entityClass, long count)
    {
        return collector.total(entityClass, count);
    }

    @Override
    public MeasurementCollector total(String module, String entity, long count)
    {
        return collector.total(module, entity, count);
    }

    @Override
    public MeasurementCollector forModule(String module)
    {
        return collector.forModule(module);
    }

    @Override
    public MeasurementCollector total(String entity, long count)
    {
        return collector.total(entity, count);
    }

    public long totalFor(String module, String entity)
    {
        return collector.totalFor(module, entity);
    }
}
