package nl.ulso.curator.addon.journal;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.extension.ExtensionBinding;
import nl.ulso.curator.query.Query;

/// Bindings of the queries provided by the journal extension.
@Module
abstract class JournalQueryModule
{
    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindTimelineQuery(TimelineQuery timelineQuery);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindPeriodQuery(PeriodQuery periodQuery);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindWeeklyQuery(WeeklyQuery weeklyQuery);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindDayNavigationQuery(DayNavigationQuery dayNavigationQuery);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindWeekNavigationQuery(WeekNavigationQuery weekNavigationQuery);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindLatestJournalNavigationQuery(LatestJournalNavigationQuery markedQuery);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindMarkedQuery(MarkedQuery markedQuery);
}
