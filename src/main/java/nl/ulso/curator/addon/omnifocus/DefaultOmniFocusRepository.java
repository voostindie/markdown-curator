package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import nl.ulso.curator.RunMode;
import nl.ulso.curator.change.ExternalChangeHandler;
import nl.ulso.curator.extension.ExtensionLifecycle;
import nl.ulso.curator.extension.ExtensionScope;
import nl.ulso.jxa.JavaScriptForAutomation;
import org.slf4j.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
/// Fetching starts when the extension is started, and stops when it is stopped. If the application
/// is not run as a daemon, but just once, then no thread is scheduled and projects are fetched once
/// in the main application thread.
@ExtensionScope
final class DefaultOmniFocusRepository
    implements OmniFocusRepository, ExtensionLifecycle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOmniFocusRepository.class);

    private static final String JXA_SCRIPT = "omnifocus-projects";
    public static final int IMMEDIATELY = 0;
    private static final int REFRESH_DELAY_MINUTES = 5;

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

    private final File database;
    private final ExternalChangeHandler externalChangeHandler;
    private final JavaScriptForAutomation javaScriptForAutomation;
    private final OmniFocusSettings settings;
    private ScheduledExecutorService refreshExecutor;

    @Inject
    public DefaultOmniFocusRepository(
        ExternalChangeHandler externalChangeHandler,
        JavaScriptForAutomation javaScriptForAutomation,
        OmniFocusSettings settings)
    {
        this.database = settings.database().toFile();
        if (!database.canRead())
        {
            throw new IllegalStateException(
                "OmniFocus database is inaccessible: " + database.getAbsolutePath());
        }
        this.externalChangeHandler = externalChangeHandler;
        this.javaScriptForAutomation = javaScriptForAutomation;
        this.settings = settings;
        this.cache = new AtomicReference<>();
        this.refreshExecutor = null;
    }

    /// Starts fetching projects from OmniFocus: in the background, every few minutes, when running
    /// as a daemon, or once, immediately, otherwise.
    @Override
    public void start()
    {
        switch (RunMode.get())
        {
            case DAEMON -> scheduleBackgroundRefresh();
            case ONCE -> reloadProjects();
            default -> throw new IllegalArgumentException(
                "Unsupported run mode: " + RunMode.get());
        }
    }

    @Override
    public void stop()
    {
        if (refreshExecutor != null)
        {
            LOGGER.debug("Shutting down OmniFocus refresh scheduler.");
            refreshExecutor.shutdownNow();
            refreshExecutor = null;
        }
    }

    private void scheduleBackgroundRefresh()
    {
        var contextMap = getCopyOfContextMap();
        refreshExecutor = newSingleThreadScheduledExecutor();
        refreshExecutor.scheduleAtFixedRate(() ->
            {
                MDC.setContextMap(contextMap);
                reloadProjects();
            },
            IMMEDIATELY,
            REFRESH_DELAY_MINUTES,
            MINUTES
        );
        LOGGER.info(
            "Scheduled background refresh of OmniFocus projects every {} minutes.",
            REFRESH_DELAY_MINUTES
        );
    }

    private void reloadProjects()
    {
        // Read the timestamp once, before fetching: a change during the fetch then triggers
        // another fetch on the next refresh, instead of being marked as seen.
        var databaseLastModified = database.lastModified();
        if (lastModified == databaseLastModified)
        {
            LOGGER.debug("No changes in the OmniFocus database; skipping fetch.");
            return;
        }
        var newProjects = fetchProjects();
        var oldProjects = cache.getAndSet(newProjects);
        lastModified = databaseLastModified;
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

    private Map<String, OmniFocusProject> fetchProjects()
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