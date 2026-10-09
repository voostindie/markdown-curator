package nl.ulso.curator.addon.newday;

import dagger.Binds;
import dagger.Module;

/// Module that produces a [NewDay] event once per day, as early as possible.
///
/// Note that at least one implementation class in a module using the [NewDayModule] must inject
/// the [NewDayAlarm] interface (e.g. in its constructor), otherwise the alarm might not be
/// constructed.
///
/// With this module installed, a [nl.ulso.curator.change.ChangeProcessor] can consume the [NewDay]
/// event, and [nl.ulso.curator.query.Query] implements can check for its presence in the the
/// changelog.
@Module
public abstract class NewDayModule
{
   @Binds
   abstract NewDayAlarm bindNewDayAlarm(DefaultNewDayAlarm alarm);
}
