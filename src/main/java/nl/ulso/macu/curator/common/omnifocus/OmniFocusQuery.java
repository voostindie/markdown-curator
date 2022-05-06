package nl.ulso.macu.curator.common.omnifocus;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static nl.ulso.macu.query.QueryResult.failure;

public class OmniFocusQuery
        implements Query
{
    private static final String PROJECT_FOLDER = "project-folder";
    private static final String OMNIFOCUS_FOLDER = "omnifocus-folder";
    private static final String IGNORED_PROJECTS = "ignored-projects";
    private static final String REFRESH_INTERVAL = "refresh-interval";
    private static final int DEFAULT_REFRESH_INTERVAL = 60000;

    private final OmniFocusRepository omniFocusRepository;
    private final Vault vault;

    public OmniFocusQuery(Vault vault)
    {
        this.omniFocusRepository = new OmniFocusRepository();
        this.vault = vault;
    }

    @Override
    public String name()
    {
        return "omnifocus";
    }

    @Override
    public String description()
    {
        return "lists inconsistencies between OmniFocus and the projects in this vault";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
                PROJECT_FOLDER, "Folder in the vault where projects are stored",
                OMNIFOCUS_FOLDER, "OmniFocus folder to select projects from",
                IGNORED_PROJECTS, "list of OmniFocus projects to ignore; defaults to empty list",
                REFRESH_INTERVAL,
                "Minimum number of milliseconds to wait for refreshes; defaults to 60000 (1 " +
                        "minute)");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var configuration = queryBlock.configuration();
        var projectFolder = configuration.string(PROJECT_FOLDER, null);
        if (projectFolder == null)
        {
            return failure("Property '" + PROJECT_FOLDER + "' is missing.");
        }
        var omniFocusFolder = configuration.string(OMNIFOCUS_FOLDER, null);
        if (omniFocusFolder == null)
        {
            return failure("Property '" + OMNIFOCUS_FOLDER + "' is missing.");
        }
        var ignoredProjects = new HashSet<>(configuration.listOfStrings(IGNORED_PROJECTS));
        var refreshInterval = configuration.integer(REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL);
        return vault.folder(projectFolder)
                .map(folder ->
                        (QueryResult) new OmniFocusQueryResult(
                                omniFocusRepository.projects(omniFocusFolder, refreshInterval),
                                folder,
                                ignoredProjects))
                .orElse(failure("Project folder not found: '" + projectFolder + "'"));
    }

    private static class OmniFocusQueryResult
            implements QueryResult
    {
        private final List<OmniFocusProject> omniFocusProjects;
        private final Folder projectFolder;
        private final Set<String> ignoredProjects;

        OmniFocusQueryResult(
                List<OmniFocusProject> omniFocusProjects, Folder projectFolder,
                Set<String> ignoredProjects)
        {
            this.omniFocusProjects = omniFocusProjects;
            this.projectFolder = projectFolder;
            this.ignoredProjects = ignoredProjects;
        }

        @Override
        public boolean isSuccess()
        {
            return true;
        }

        @Override
        public String toMarkdown()
        {
            var builder = new StringBuilder();
            var missingPages = omniFocusProjects.stream()
                    .map(OmniFocusProject::name)
                    .filter(name -> !ignoredProjects.contains(name))
                    .filter(name -> projectFolder.document(name).isEmpty())
                    .toList();
            if (!missingPages.isEmpty())
            {
                builder.append("### Projects without a matching page");
                builder.append(lineSeparator());
                builder.append(lineSeparator());
                missingPages.forEach(
                        name -> builder.append("- ").append(name).append(lineSeparator()));
                builder.append(lineSeparator());
            }
            var projectSet = omniFocusProjects.stream()
                    .map(OmniFocusProject::name)
                    .collect(Collectors.toSet());
            var missingProjects = projectFolder.documents().stream()
                    .filter(document -> !projectSet.contains(document.name()))
                    .toList();
            if (!missingProjects.isEmpty())
            {
                builder.append("### Pages without a matching project");
                builder.append(lineSeparator());
                builder.append(lineSeparator());
                missingProjects.forEach(
                        document -> builder.append("- ").append(document.link())
                                .append(lineSeparator()));
                builder.append(lineSeparator());
            }
            if (missingPages.isEmpty() && missingProjects.isEmpty())
            {
                builder.append("### All good!");
                builder.append(lineSeparator());
                builder.append(lineSeparator());
                builder.append("There are no inconsistencies to report.");
                builder.append(lineSeparator());
            }
            return builder.toString().trim();
        }
    }
}
