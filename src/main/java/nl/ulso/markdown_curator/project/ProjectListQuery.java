package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.Document;

import java.util.List;
import java.util.Map;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.comparingInt;
import static nl.ulso.markdown_curator.project.Attribute.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.Attribute.LEAD;
import static nl.ulso.markdown_curator.project.Attribute.PRIORITY;
import static nl.ulso.markdown_curator.project.Attribute.STATUS;

public final class ProjectListQuery
        implements Query
{
    private final ProjectRepository projectRepository;
    private final GeneralMessages messages;
    private final QueryResultFactory resultFactory;

    private enum Format
    {
        LIST,
        TABLE
    }

    private final static Map<String, Format> FORMATS =
            Map.of("list", Format.LIST, "table", Format.TABLE);

    @Inject
    public ProjectListQuery(
            ProjectRepository projectRepository, GeneralMessages messages,
            QueryResultFactory resultFactory)
    {
        this.projectRepository = projectRepository;
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
        var projects = projectRepository.projects().stream()
                .sorted(comparingInt(p -> p.attributeValue(PRIORITY).orElse(MAX_VALUE)));
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
                                    project.attributeValue(PRIORITY)
                                            .map(p -> Integer.toString(p))
                                            .orElse(messages.projectPriorityUnknown()),
                                    messages.projectName(),
                                    project.document().link(),
                                    messages.projectLead(),
                                    project.attributeValue(LEAD)
                                            .map(Document::link)
                                            .orElse(messages.projectLeadUnknown()),
                                    messages.projectLastModified(),
                                    project.attributeValue(LAST_MODIFIED)
                                            .map(d -> "[[" + d + "]]")
                                            .orElse(messages.projectDateUnknown()),
                                    messages.projectStatus(),
                                    project.attributeValue(STATUS)
                                            .orElse(messages.projectStatusUnknown())))
                            .toList());
        };
    }
}
