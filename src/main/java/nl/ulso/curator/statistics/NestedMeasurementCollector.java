package nl.ulso.curator.statistics;

final class NestedMeasurementCollector
    implements MeasurementCollector
{
    private final MeasurementCollector parent;
    private final String module;

    public NestedMeasurementCollector(MeasurementCollector parent, String module)
    {
        this.parent = parent;
        this.module = module;
    }

    @Override
    public MeasurementCollector total(Class<?> entityClass, long count)
    {
        parent.total(entityClass, count);
        return this;
    }

    @Override
    public MeasurementCollector total(String module, String entity, long count)
    {
        if (module.contentEquals(this.module))
        {
            return total(entity, count);
        }
        parent.total(module, entity, count);
        return this;
    }

    @Override
    public MeasurementCollector forModule(String module)
    {
        if (module.contentEquals(this.module))
        {
            return this;
        }
        return parent.forModule(module);
    }

    @Override
    public MeasurementCollector total(String entity, long count)
    {
        parent.total(module, entity, count);
        return this;
    }
}
