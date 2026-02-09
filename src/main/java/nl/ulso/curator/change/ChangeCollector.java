package nl.ulso.curator.change;

import java.util.ArrayList;

/// Collector for changes by the [ChangeProcessorTemplate] during change processing. The collected
/// changes are published to the changelog at the end of the run.
public interface ChangeCollector
{
    static ChangeCollector newChangeCollector()
    {
        return new DefaultChangeCollector(new ArrayList<>());
    }

    <T> void add(Change<T> change);

    <T> void create(T newValue, Class<T> payloadType);

    <T> void update(T value, Class<T> payloadType);

    <T> void update(T oldValue, T newValue, Class<T> payloadType);

    <T> void delete(T oldValue, Class<T> payloadType);

    Changelog changelog();
}
