package nl.ulso.curator.statistics;

import java.util.Collection;

/// Registry for [MeasurementTracker]s that come and go at runtime, next to the trackers that are
/// statically configured.
public interface MeasurementTrackerRegistry
{
    /// Replaces all dynamically registered trackers with the given trackers.
    void replaceDynamicTrackers(Collection<MeasurementTracker> trackers);
}
