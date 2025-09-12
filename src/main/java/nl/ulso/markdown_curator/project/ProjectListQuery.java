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

/**
 * Lists all active projects, either in a simple list, or in a table.
 */
public final class ProjectListQuery
        implements Query
{
    private final ProjectPropertyRepository projectPropertyRepository;
    private final GeneralMessages messages;
    private final QueryResultFactory resultFactory;

    private enum Format
    {
        LIST,
        TABLE
    }

    private static final Map<String, Format> FORMATS =
            Map.of("list", Format.LIST, "table", Format.TABLE);

    @Inject
    public ProjectListQuery(
            ProjectPropertyRepository projectPropertyRepository,
            GeneralMessages messages,
            QueryResultFactory resultFactory)
    {
        this.projectPropertyRepository = projectPropertyRepository;
        this.messages = messages;
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "projects";
    }

    @Override
    public String description()
    {
        return "outputs all active projects";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("format", "Output format: 'list' (default) or 'table'.");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var format = FORMATS.get(definition.configuration().string("format", "list"));
        if (format == null)
        {
            return resultFactory.error("Unsupported format");
        }
        var projects = projectPropertyRepository.projects().stream()
                .sorted(comparingInt(p -> projectPropertyRepository.propertyValue(p,
                                projectPropertyRepository.property(PRIORITY))
                        .map(i -> ((Integer) i))
                        .orElse(MAX_VALUE)));
        return switch (format)
        {
            case LIST -> resultFactory.unorderedList(projects
                    .map((Project project) -> project.document().link())
                    .toList());
            case TABLE -> resultFactory.table(
                    List.of(messages.projectPriority(),
                            messages.projectName(),
                            messages.projectLead(),
                            messages.projectLastModified(),
                            messages.projectStatus()),
                    projects.map((Project project) -> Map.of(
                                    messages.projectPriority(),
                                    projectPropertyRepository.propertyValue(project, PRIORITY)
                                            .map(i -> (Integer) i)
                                            .map(p -> Integer.toString(p))
                                            .orElse(messages.projectPriorityUnknown()),
                                    messages.projectName(),
                                    project.document().link(),
                                    messages.projectLead(),
                                    projectPropertyRepository.propertyValue(project, LEAD)
                                            .map(d -> (Document) d)
                                            .map(Document::link)
                                            .orElse(messages.projectLeadUnknown()),
                                    messages.projectLastModified(),
                                    projectPropertyRepository.propertyValue(project, LAST_MODIFIED)
                                            .map(d -> (LocalDate) d)
                                            .map(d -> "[[" + d + "]]")
                                            .orElse(messages.projectDateUnknown()),
                                    messages.projectStatus(),
                                    projectPropertyRepository.propertyValue(project, STATUS)
                                            .map(s -> (String) s)
                                            .orElse(messages.projectStatusUnknown())))
                            .toList());
        };
    }
}
