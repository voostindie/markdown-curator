package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import nl.ulso.curator.RunMode;
import nl.ulso.curator.change.ExternalChangeHandler;
import nl.ulso.jxa.JavaScriptForAutomation;
import org.slf4j.*;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.Runtime.getRuntime;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toMap;
import static nl.ulso.curator.addon.omnifocus.OmniFocusUpdate.OMNIFOCUS_CHANGE;
import static nl.ulso.curator.addon.omnifocus.Status.ACTIVE;
import static nl.ulso.curator.addon.omnifocus.Status.ON_HOLD;
import static org.slf4j.MDC.getCopyOfContextMap;

/// Fetches projects from a folder in OmniFocus.
///
/// This implementation uses JXA scripting. Data is refreshed every 5 minutes, independently of the
/// rest of the system, to ensure system overall throughput is not impacted. If the OmniFocus
/// database hasn't changed (based on the modification timestamp of the database folder), refresh is
/// skipped. If, after a refresh, a change is detected to the set of projects in memory, a change
/// object is published, requesting the system to process the update.
///
/// If the application is not run as a daemon, but just once, then no thread is scheduled and
/// projects are fetched once in the main application thread.
@Singleton
final class DefaultOmniFocusRepository
    implements OmniFocusRepository
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOmniFocusRepository.class);

    private static final String JXA_SCRIPT = "omnifocus-projects";
    public static final int IMMEDIATELY = 0;
    private static final int REFRESH_DELAY_MINUTES = 5;

    private final ScheduledExecutorService refreshExecutor;
    /// Because fetching projects from OmniFocus is a scheduled activity, it might run in parallel
    /// with access in the [OmniFocusProjectAttributeValueProducer]. In practice this can't happen
    /// as the access from the [OmniFocusProjectAttributeValueProducer] always comes after the
    /// refresh; synchronization is done through the [OmniFocusUpdate] change object: this registry
    /// creates that object. No refresh, no object, no access. However, it's better to be safe than
    /// sorry.
    private final AtomicReference<Map<String, OmniFocusProject>> cache;
    private long lastModified = 0L;

    /// The filtering on statuses is ideally done in the JXA script to limit the data pulled from
    /// OmniFocus, but this broke in OmniFocus 4.3.3. Now the filtering is in here, in the client.
    private static final Set<Status> SELECTED_STATUSES = Set.of(ACTIVE, ON_HOLD);

    @Inject
    public DefaultOmniFocusRepository(
        OmniFocusDatabase omniFocusDatabase,
        ExternalChangeHandler externalChangeHandler,
        JavaScriptForAutomation javaScriptForAutomation,
        OmniFocusSettings settings)
    {
        if (!omniFocusDatabase.isAccessible())
        {
            throw new IllegalStateException(
                "OmniFocus database is inaccessible: " + omniFocusDatabase.path());

        }
        this.cache = new AtomicReference<>();
        switch (RunMode.get())
        {
            case DAEMON ->
            {
                this.refreshExecutor = newSingleThreadScheduledExecutor();
                scheduleBackgroundRefresh(
                    omniFocusDatabase,
                    externalChangeHandler,
                    javaScriptForAutomation,
                    settings
                );
            }
            case ONCE ->
            {
                this.refreshExecutor = null;
                reloadProjects(
                    omniFocusDatabase,
                    externalChangeHandler,
                    javaScriptForAutomation,
                    settings
                );
            }
            default -> throw new IllegalArgumentException(
                "Unsupported run mode: " + RunMode.get());
        }
    }

    private void scheduleBackgroundRefresh(
        OmniFocusDatabase omniFocusDatabase,
        ExternalChangeHandler externalChangeHandler,
        JavaScriptForAutomation javaScriptForAutomation,
        OmniFocusSettings settings)
    {
        var contextMap = getCopyOfContextMap();
        var task = refreshExecutor.scheduleAtFixedRate(() ->
            {
                MDC.setContextMap(contextMap);
                reloadProjects(
                    omniFocusDatabase,
                    externalChangeHandler,
                    javaScriptForAutomation,
                    settings
                );
            },
            IMMEDIATELY,
            REFRESH_DELAY_MINUTES,
            MINUTES
        );
        LOGGER.info(
            "Scheduled background refresh of OmniFocus projects every {} minutes.",
            REFRESH_DELAY_MINUTES
        );
        getRuntime().addShutdownHook(
            new Thread(() ->
            {
                MDC.setContextMap(contextMap);
                LOGGER.debug("Shutting down OmniFocus refresh scheduler.");
                task.cancel(true);
            })
        );
    }

    private void reloadProjects(
        OmniFocusDatabase omniFocusDatabase, ExternalChangeHandler externalChangeHandler,
        JavaScriptForAutomation javaScriptForAutomation, OmniFocusSettings settings)
    {
        if (lastModified == omniFocusDatabase.lastModified())
        {
            LOGGER.debug("No changes in the OmniFocus database; skipping fetch.");
            return;
        }
        var newProjects = fetchProjects(javaScriptForAutomation, settings);
        var oldProjects = cache.getAndSet(newProjects);
        lastModified = omniFocusDatabase.lastModified();
        if (oldProjects == null)
        {
            LOGGER.debug("Initial fetch from OmniFocus completed.");
            return;
        }
        if (newProjects.equals(oldProjects))
        {
            LOGGER.debug("No changes in projects from OmniFocus; skipping refresh.");
            return;
        }
        LOGGER.info("Relevant OmniFocus changes detected. Triggering a refresh.");
        externalChangeHandler.process(OMNIFOCUS_CHANGE);
    }

    private Map<String, OmniFocusProject> fetchProjects(
        JavaScriptForAutomation javaScriptForAutomation, OmniFocusSettings settings)
    {
        LOGGER.debug("Fetching OmniFocus projects in folder '{}'.", settings.omniFocusFolder());
        var priorityCounter = new AtomicInteger(IMMEDIATELY);
        JsonArray array;
        try
        {
            array = javaScriptForAutomation.runScriptForArray(
                JXA_SCRIPT,
                settings.omniFocusFolder()
            );
        }
        catch (IllegalStateException e)
        {
            LOGGER.error("Failed to fetch OmniFocus projects: {}", e.getMessage());
            return emptyMap();
        }
        return array.stream()
            .map(JsonValue::asJsonObject)
            .map(object ->
                new OmniFocusProject(
                    object.getString("id"),
                    object.getString("name"),
                    Status.fromString(object.getString("status"))
                )
            )
            .filter(project -> settings.includePredicate().test(project.name()))
            .filter(project -> SELECTED_STATUSES.contains(project.status()))
            .map(project -> project.withUpdatedPriority(priorityCounter.incrementAndGet()))
            .collect(toMap(OmniFocusProject::name, Function.identity()));
    }

    /// If, at system start, the request for data comes before the data from OmniFocus is available,
    /// the system spins and waits.
    ///
    /// @see OmniFocusInitializer
    void waitForInitialFetchToComplete()
    {
        Map<String, OmniFocusProject> result = null;
        while (result == null)
        {
            result = cache.get();
        }
        LOGGER.debug("Fetched {} projects from OmniFocus.", result.size());
    }

    public Collection<OmniFocusProject> projects()
    {
        return cache.get().values();
    }

    public Optional<OmniFocusProject> projectNamed(String name)
    {
        return Optional.ofNullable(cache.get().get(name));
    }
}