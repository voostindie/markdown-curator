package nl.ulso.curator.addon.newday;

import nl.ulso.curator.change.Change;

import static nl.ulso.curator.change.Change.create;

/// Event that is triggered once a day, as close to midnight as possible, when the machine is
/// active.
///
/// This event is NOT public on purpose to force users of this module to use the [NewDayAlarm]
/// interface. To use that, they must inject it, and by injecting it, the daily alarm is guaranteed
/// to be set.
record NewDay()
{
    static final Change<NewDay> NEW_DAY = create(new NewDay(), NewDay.class);

    @Override
    public String toString()
    {
        return "⏰";
    }
}
