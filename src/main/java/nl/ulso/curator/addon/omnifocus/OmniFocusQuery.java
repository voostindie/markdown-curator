package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import nl.ulso.curator.addon.project.Project;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.net.URLEncoder.encode;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static nl.ulso.curator.change.Change.isPayloadType;

/// Reports on inconsistencies between OmniFocus and the projects in this vault.
public final class OmniFocusQuery
    implements Query
{
    private final OmniFocusRepository omniFocusRepository;
    private final ProjectRepository projectRepository;
    private final OmniFocusSettings settings;
    private final OmniFocusMessages messages;
    private final QueryResultFactory queryResultFactory;

    @Inject
    public OmniFocusQuery(
        OmniFocusRepository omniFocusRepository, ProjectRepository projectRepository,
        OmniFocusSettings settings,
        OmniFocusMessages messages,
        QueryResultFactory queryResultFactory)
    {
        this.omniFocusRepository = omniFocusRepository;
        this.projectRepository = projectRepository;
        this.settings = settings;
        this.messages = messages;
        this.queryResultFactory = queryResultFactory;
    }

    @Override
    public String name()
    {
        return "omnifocus";
    }

    @Override
    public String description()
    {
        return "Lists inconsistencies between OmniFocus and the projects in this vault.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return changelog.changes().anyMatch(
            isPayloadType(Project.class).or(isPayloadType(OmniFocusUpdate.class))
        );
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var projectsWithoutDocuments = collectOmniFocusProjectsWithoutDocuments();
        var documentsWithoutProjects = collectDocumentsWithoutOmniFocusProjects();
        if (projectsWithoutDocuments.isEmpty() && documentsWithoutProjects.isEmpty())
        {
            return queryResultFactory.empty();
        }
        return queryResultFactory.string(
            generateReport(projectsWithoutDocuments, documentsWithoutProjects)
        );
    }

    private List<OmniFocusProject> collectOmniFocusProjectsWithoutDocuments()
    {
        var documentNames = projectRepository.projectsByName().keySet();
        var omniFocusProjects = omniFocusRepository.projects();
        return omniFocusProjects.stream()
            .filter(project -> !documentNames.contains(project.name()))
            .toList();
    }

    private List<Document> collectDocumentsWithoutOmniFocusProjects()
    {
        var omniFocusProjects = omniFocusRepository.projects().stream()
            .map(OmniFocusProject::name)
            .collect(toSet());
        return projectRepository.projects().stream()
            .filter(project -> !omniFocusProjects.contains(project.name()))
            .map(Project::document)
            .toList();
    }

    private String generateReport(
        List<OmniFocusProject> projectsWithoutDocuments,
        List<Document> documentsWithoutProjects)
    {
        var builder = new StringBuilder();
        if (!projectsWithoutDocuments.isEmpty())
        {
            reportProjectsWithoutDocuments(builder, projectsWithoutDocuments);
        }
        if (!documentsWithoutProjects.isEmpty())
        {
            reportDocumentsWithoutProjects(builder, documentsWithoutProjects);
        }
        return builder.toString().trim();
    }

    private void reportProjectsWithoutDocuments(
        StringBuilder builder, List<OmniFocusProject> projectsWithoutDocuments)
    {
        builder.append("### ")
            .append(messages.projectsWithoutDocumentsTitle())
            .append(lineSeparator())
            .append(lineSeparator());
        projectsWithoutDocuments.forEach(project ->
            builder.append("- [[")
                .append(project.name())
                .append("]]")
                .append(lineSeparator()));
        builder.append(lineSeparator());
    }

    private void reportDocumentsWithoutProjects(
        StringBuilder builder, List<Document> documentsWithoutProjects)
    {
        builder.append("### ")
            .append(messages.documentsWithoutProjectsTitle())
            .append(lineSeparator())
            .append(lineSeparator());
        documentsWithoutProjects.forEach(document ->
            builder.append("- ")
                .append(document.link())
                .append(" - [")
                .append(messages.createProjectInOmniFocus())
                .append("](omnifocus:///paste")
                .append("?index=1")
                .append("&target=/folder/")
                .append(urlEncode(settings.omniFocusFolder()))
                .append("&content=")
                .append(urlEncode(document.name() + ":"))
                .append(")")
                .append(lineSeparator()));
        builder.append(lineSeparator());
    }

    private String urlEncode(String value)
    {
        return encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
