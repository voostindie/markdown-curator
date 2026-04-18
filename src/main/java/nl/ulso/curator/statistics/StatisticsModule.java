package nl.ulso.curator.statistics;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;

import java.util.Set;

/// Collects statistics from all the system and logs them consistently.
///
/// To expose statistics from a module, implement the [MeasurementTracker] interface (as often as
/// needed) and bind the implementations into a set in the Dagger module.
@Module
public abstract class StatisticsModule
{
    /// To add a tracker, configure it so that it [Binds] [IntoSet] of [MeasurementTracker].
    @Multibinds
    abstract Set<MeasurementTracker> bindAllMeasurementTrackers();

    @Binds
    abstract Statistics bindStatistics(DefaultStatistics statistics);
}
