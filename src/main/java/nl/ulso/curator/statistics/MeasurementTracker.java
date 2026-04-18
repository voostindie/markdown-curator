package nl.ulso.curator.statistics;

/// Collects measurements from some module.
///
/// To contribute statistics to the system, implement this interface and bind it to the set of
/// trackers in your Dagger module.
public interface MeasurementTracker
{
    void collectMeasurements(MeasurementCollector collector);
}
