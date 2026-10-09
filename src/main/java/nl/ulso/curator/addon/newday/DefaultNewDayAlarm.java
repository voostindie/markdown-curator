package nl.ulso.curator.addon.newday;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.RunMode;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.change.ExternalChangeHandler;
import org.slf4j.*;

import java.time.LocalDate;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static nl.ulso.curator.addon.newday.NewDay.NEW_DAY;
import static org.slf4j.MDC.getCopyOfContextMap;

/// The new day alarm schedules a task every few minutes, but only really does something once a day.
/// This is to ensure that the start of a new day is not missed, for example, when the machine is on
/// standby.
///
/// When the alarm goes off, it publishes a [NewDay] event to the changelog.
///
/// The question is how often this job should run. It's now scheduled every 15 minutes, which means
/// that on a computer that's on 24 hours, it only does something useful 1 time out of the 96 times
/// it is executed. On the other hand, we also want to run the job as close to midnight as possible,
/// and those other 95 times it barely does anything: comparing the current date to a date kept in
/// memory. Every 15 minutes seems like a good balance.
@Singleton
final class DefaultNewDayAlarm
    implements NewDayAlarm
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNewDayAlarm.class);
    private static final int INITIAL_DELAY_MINUTES = 15;
    private static final int REFRESH_DELAY_MINUTES = 15;

    private final ScheduledExecutorService alarmClock;
    private LocalDate lastRun;

    @Inject
    public DefaultNewDayAlarm(ExternalChangeHandler externalChangeHandler)
    {
        switch (RunMode.get())
        {
            case DAEMON ->
            {
                this.lastRun = LocalDate.now();
                this.alarmClock = newSingleThreadScheduledExecutor();
                scheduleDailyMorningAlarm(externalChangeHandler);
            }
            case ONCE ->
            {
                // Do nothing. In a one-off run mode, the alarm is not needed.
                this.lastRun = null;
                this.alarmClock = null;
            }
            default -> throw new IllegalArgumentException("Unsupported run mode: " + RunMode.get());
        }
    }

    private void scheduleDailyMorningAlarm(ExternalChangeHandler externalChangeHandler)
    {
        var contextMap = getCopyOfContextMap();
        var task = alarmClock.scheduleAtFixedRate(() ->
            {
                MDC.setContextMap(contextMap);
                LOGGER.trace("Checking if it's time to announce a new day.");
                if (lastRun.isBefore(LocalDate.now()))
                {
                    LOGGER.info("Publishing an event for the start of a new day.");
                    externalChangeHandler.process(NEW_DAY);
                    lastRun = LocalDate.now();
                }
            },
            INITIAL_DELAY_MINUTES,
            REFRESH_DELAY_MINUTES,
            MINUTES
        );
        LOGGER.info(
            "Scheduled background check for the start of a new day every {} minutes.",
            REFRESH_DELAY_MINUTES
        );
        getRuntime().addShutdownHook(
            new Thread(() ->
            {
                MDC.setContextMap(contextMap);
                LOGGER.debug("Shutting down the daily morning alarm.");
                task.cancel(true);
            })
        );
    }

    @Override
    public boolean didAlarmTrigger(Changelog changelog)
    {
        return changelog.changesFor(NewDay.class).findFirst().isPresent();
    }
}
