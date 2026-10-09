package nl.ulso.curator.addon.newday;

import nl.ulso.curator.RunMode;
import nl.ulso.curator.change.ExternalChangeHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static nl.ulso.curator.RunMode.DAEMON;
import static nl.ulso.curator.RunModeTestAccessor.setRunMode;
import static nl.ulso.curator.addon.newday.NewDay.NEW_DAY;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static nl.ulso.curator.change.Reset.RESET;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultNewDayAlarmTest
{
    private static final ExternalChangeHandler DUMMY_HANDLER = _ -> {};

    @AfterEach
    void resetRunMode()
    {
        setRunMode(RunMode.ONCE);
    }

    @Test
    void alarmDoesNotTriggerOnEmptyChangelog()
    {
        var alarm = new DefaultNewDayAlarm(DUMMY_HANDLER);
        assertThat(alarm.didAlarmTrigger(emptyChangelog())).isFalse();
    }

    @Test
    void alarmDoesNotTriggerOnOtherChanges()
    {
        var alarm = new DefaultNewDayAlarm(DUMMY_HANDLER);
        assertThat(alarm.didAlarmTrigger(changelogFor(RESET))).isFalse();
    }

    @Test
    void alarmTriggersWhenNewDayEventInChangelog()
    {
        var alarm = new DefaultNewDayAlarm(DUMMY_HANDLER);
        assertThat(alarm.didAlarmTrigger(changelogFor(NEW_DAY))).isTrue();
    }

    @Test
    void alarmConstructedInDaemonMode()
    {
        setRunMode(DAEMON);
        var alarm = new DefaultNewDayAlarm(DUMMY_HANDLER);
        assertThat(alarm.didAlarmTrigger(changelogFor(NEW_DAY))).isTrue();
    }
}
