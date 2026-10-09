package nl.ulso.curator.addon.newday;

import nl.ulso.curator.change.Changelog;

public interface NewDayAlarm
{
    /// @return true if the alarm triggered for this changelog.
    boolean didAlarmTrigger(Changelog changelog);
}
