package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparingInt;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.LAST_MODIFIED;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.LEAD;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.PRIORITY;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.STATUS;
import static nl.ulso.curator.change.Change.isPayloadType;

/// Lists all projects lead by a specific party (contact, team, ...document).
public final class ProjectLeadQuery
    implements Query
{
    private final ProjectRepository projectRepository;
    private final ProjectAttributeRepository projectAttributeRepository;
    private final GeneralMessages messages;
    private final QueryResultFactory resultFactory;

    @Inject
    public ProjectLeadQuery(
        ProjectRepository projectRepository,
        ProjectAttributeRepository projectAttributeRepository,
        GeneralMessages messages,
        QueryResultFactory resultFactory)
    {
        this.projectRepository = projectRepository;
        this.projectAttributeRepository = projectAttributeRepository;
        this.messages = messages;
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "projectlead";
    }

    @Override
    public String description()
    {
        return "Outputs all active projects lead by a specific contact.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("lead",
            "Project lead (contact) to select; defaults to the name of the current document."
        );
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return changelog.changes()
            .anyMatch(isPayloadType(ProjectAttributeRepositoryUpdate.class));
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var lead = definition.configuration()
            .string("lead", definition.document().name());
        var projects = projectRepository.projects().stream()
            .filter(project -> projectAttributeRepository.valueOf(project, LEAD)
                .map(d -> ((Document) d).name().contentEquals(lead))
                .orElse(false))
            .sorted(comparingInt(p -> projectAttributeRepository.valueOf(p, PRIORITY)
                .map(i -> (Integer) i).orElse(MAX_VALUE)))
            .toList();
        return resultFactory.table(
            List.of(messages.projectPriority(),
                messages.projectName(),
                messages.projectLastModified(),
                messages.projectStatus()
            ),
            projects.stream()
                .map((Project project) -> Map.of(
                    messages.projectPriority(),
                    projectAttributeRepository.valueOf(project, PRIORITY)
                        .map(i -> (Integer) i)
                        .map(p -> Integer.toString(p))
                        .orElse("-"),
                    messages.projectName(),
                    project.document().link(),
                    messages.projectLastModified(),
                    projectAttributeRepository.valueOf(project, LAST_MODIFIED)
                        .map(d -> (LocalDate) d)
                        .map(d -> "[[" + d + "]]")
                        .orElse("-"),
                    messages.projectStatus(),
                    projectAttributeRepository.valueOf(project, STATUS)
                        .map(s -> (String) s)
                        .orElse("-")
                ))
                .toList()
        );
    }
}
