package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import nl.ulso.markdown_curator.query.*;

import java.util.List;
import java.util.Map;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparingInt;
import static nl.ulso.markdown_curator.project.Attribute.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.Attribute.PRIORITY;
import static nl.ulso.markdown_curator.project.Attribute.STATUS;

/**
 * Lists all projects lead by a specific party (contact, team, ...document).
 */
public final class ProjectLeadQuery
        implements Query
{
    private final ProjectRepository repository;
    private final GeneralMessages messages;
    private final QueryResultFactory resultFactory;

    @Inject
    public ProjectLeadQuery(
            ProjectRepository repository, GeneralMessages messages,
            QueryResultFactory resultFactory)
    {
        this.repository = repository;
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
        return "Outputs all active projects lead by a specific party.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("lead", "Project lead to select; defaults to the name of the document.");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var lead = definition.configuration()
                .string("lead", definition.document().name());
        var projects = repository.projects().stream()
                .filter(project -> project.attributeValue(Attribute.LEAD)
                        .map(d -> d.name().contentEquals(lead))
                        .orElse(false))
                .sorted(comparingInt(p -> p.attributeValue(PRIORITY).orElse(MAX_VALUE)))
                .toList();
        return resultFactory.table(
                List.of(messages.projectPriority(),
                        messages.projectName(),
                        messages.projectLastModified(),
                        messages.projectStatus()),
                projects.stream()
                        .map((Project project) -> Map.of(
                                messages.projectPriority(),
                                project.attributeValue(PRIORITY)
                                        .map(p -> Integer.toString(p))
                                        .orElse("-"),
                                messages.projectName(),
                                project.document().link(),
                                messages.projectLastModified(),
                                project.attributeValue(LAST_MODIFIED)
                                        .map(d -> "[[" + d + "]]")
                                        .orElse("-") ,
                                messages.projectStatus(),
                                project.attributeValue(STATUS).orElse("-")))
                        .toList()
        );
    }
}
