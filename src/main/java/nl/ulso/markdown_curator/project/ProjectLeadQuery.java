package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparingInt;
import static nl.ulso.markdown_curator.project.ProjectProperty.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.ProjectProperty.LEAD;
import static nl.ulso.markdown_curator.project.ProjectProperty.PRIORITY;
import static nl.ulso.markdown_curator.project.ProjectProperty.STATUS;

/// Lists all projects lead by a specific party (contact, team, ...document).
public final class ProjectLeadQuery
    implements Query
{
    private final ProjectPropertyRepository projectPropertyRepository;
    private final GeneralMessages messages;
    private final QueryResultFactory resultFactory;

    @Inject
    public ProjectLeadQuery(
        ProjectPropertyRepository projectPropertyRepository,
        GeneralMessages messages, QueryResultFactory resultFactory)
    {
        this.projectPropertyRepository = projectPropertyRepository;
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
        return Map.of("lead", "Project lead (contact) to select; defaults to the name of the current document.");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var lead = definition.configuration()
            .string("lead", definition.document().name());
        var projects = projectPropertyRepository.projects().stream()
            .filter(project -> projectPropertyRepository.propertyValue(project, LEAD)
                .map(d -> ((Document) d).name().contentEquals(lead))
                .orElse(false))
            .sorted(comparingInt(p -> projectPropertyRepository.propertyValue(p, PRIORITY)
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
                    projectPropertyRepository.propertyValue(project, PRIORITY)
                        .map(i -> (Integer) i)
                        .map(p -> Integer.toString(p))
                        .orElse("-"),
                    messages.projectName(),
                    project.document().link(),
                    messages.projectLastModified(),
                    projectPropertyRepository.propertyValue(project, LAST_MODIFIED)
                        .map(d -> (LocalDate) d)
                        .map(d -> "[[" + d + "]]")
                        .orElse("-"),
                    messages.projectStatus(),
                    projectPropertyRepository.propertyValue(project, STATUS)
                        .map(s -> (String) s)
                        .orElse("-")
                ))
                .toList()
        );
    }
}
