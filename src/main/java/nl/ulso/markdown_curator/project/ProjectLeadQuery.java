package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparingInt;
import static nl.ulso.markdown_curator.Change.isObjectType;
import static nl.ulso.markdown_curator.project.AttributeDefinition.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.AttributeDefinition.LEAD;
import static nl.ulso.markdown_curator.project.AttributeDefinition.PRIORITY;
import static nl.ulso.markdown_curator.project.AttributeDefinition.STATUS;

/// Lists all projects lead by a specific party (contact, team, ...document).
public final class ProjectLeadQuery
    implements Query
{
    private final AttributeRegistry attributeRegistry;
    private final GeneralMessages messages;
    private final QueryResultFactory resultFactory;

    @Inject
    public ProjectLeadQuery(
        AttributeRegistry attributeRegistry,
        GeneralMessages messages,
        QueryResultFactory resultFactory)
    {
        this.attributeRegistry = attributeRegistry;
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
            .anyMatch(isObjectType(AttributeRegistryUpdate.class));
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var lead = definition.configuration()
            .string("lead", definition.document().name());
        var projects = attributeRegistry.projects().stream()
            .filter(project -> attributeRegistry.attributeValue(project, LEAD)
                .map(d -> ((Document) d).name().contentEquals(lead))
                .orElse(false))
            .sorted(comparingInt(p -> attributeRegistry.attributeValue(p, PRIORITY)
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
                    attributeRegistry.attributeValue(project, PRIORITY)
                        .map(i -> (Integer) i)
                        .map(p -> Integer.toString(p))
                        .orElse("-"),
                    messages.projectName(),
                    project.document().link(),
                    messages.projectLastModified(),
                    attributeRegistry.attributeValue(project, LAST_MODIFIED)
                        .map(d -> (LocalDate) d)
                        .map(d -> "[[" + d + "]]")
                        .orElse("-"),
                    messages.projectStatus(),
                    attributeRegistry.attributeValue(project, STATUS)
                        .map(s -> (String) s)
                        .orElse("-")
                ))
                .toList()
        );
    }
}
