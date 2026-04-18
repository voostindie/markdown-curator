package nl.ulso.curator.statistics;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

final class DefaultMeasurementCollector
    implements MeasurementCollector
{
    private final Map<String, Map<String, Long>> measurements;

    DefaultMeasurementCollector()
    {
        this.measurements = new HashMap<>();
    }

    @Override
    public MeasurementCollector total(Class<?> entityClass, long count)
    {
        var packageName = entityClass.getPackageName();
        var module = packageName.substring(packageName.lastIndexOf('.') + 1);
        var entity = entityClass.getSimpleName().toLowerCase();
        return total(module, entity, count);
    }

    @Override
    public MeasurementCollector total(String module, String entity, long count)
    {
        var moduleMeasurements = measurements.computeIfAbsent(module, _ -> new HashMap<>());
        moduleMeasurements.put(requireNonNull(entity), count);
        return this;
    }

    @Override
    public MeasurementCollector forModule(String module)
    {
        return new NestedMeasurementCollector(this, module);
    }

    @Override
    public MeasurementCollector total(String entity, long count)
    {
        throw new IllegalArgumentException("Module must be set before calling total");
    }

    long totalFor(String module, String entity)
    {
        return measurements.getOrDefault(module, emptyMap()).getOrDefault(entity, -1L);
    }

    Map<String, Map<String, Long>> measurements()
    {
        return measurements;
    }
}
