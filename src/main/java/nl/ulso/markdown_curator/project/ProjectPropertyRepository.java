package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.FrontMatterUpdateCollector;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.event.*;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Model to keep track of project properties and update front matter accordingly.
 * <p/>
 * Project properties are in a complex system, with plugins that can override how and when
 * properties are computed. That is why this model comes last, and always does a complete
 * refresh.
 * <p/>
 * Because this model always goes last, there is no point in depending on this model in other
 * models! It can be used in queries.
 */
@Singleton
public class ProjectPropertyRepository
        extends DataModelTemplate
{
    private final Map<String, ProjectProperty> projectProperties;
    private final ProjectRepository projectRepository;
    private final ProjectPropertyResolverRegistry projectPropertyResolverRegistry;
    private final FrontMatterUpdateCollector frontMatterUpdateCollector;
    private final Map<Project, Map<ProjectProperty, Object>> projectPropertyValues;

    @Inject
    ProjectPropertyRepository(
            Map<String, ProjectProperty> projectProperties,
            ProjectRepository projectRepository,
            ProjectPropertyResolverRegistry projectPropertyResolverRegistry,
            FrontMatterUpdateCollector frontMatterUpdateCollector)
    {
        this.projectProperties = Collections.unmodifiableMap(projectProperties);
        this.projectRepository = projectRepository;
        this.projectPropertyResolverRegistry = projectPropertyResolverRegistry;
        this.frontMatterUpdateCollector = frontMatterUpdateCollector;
        this.projectPropertyValues = new HashMap<>();
    }

    public ProjectRepository projectRepository()
    {
        return projectRepository;
    }

    public Map<String, ProjectProperty> projectProperties()
    {
        return projectProperties;
    }

    public Collection<Project> projects()
    {
        return projectRepository.projects();
    }

    public Optional<Project> projectFor(Document document)
    {
        return projectRepository.projectFor(document);
    }

    public ProjectProperty property(String propertyName)
    {
        return requireNonNull(projectProperties.get(propertyName));
    }

    @Override
    public void fullRefresh()
    {
        projectPropertyValues.clear();
        projectRepository.projects().forEach(this::processProject);
    }

    private void processProject(Project project)
    {
        var properties = projectPropertyValues.computeIfAbsent(project, key -> new HashMap<>(projectProperties.size()));
        frontMatterUpdateCollector.updateFrontMatterFor(project.document(), dictionary -> {
            for (ProjectProperty property : projectProperties.values())
            {
                resolvePropertyValue(project, property).ifPresentOrElse(value ->
                        {
                            properties.put(property, value);
                            dictionary.setProperty(
                                    property.frontMatterProperty(),
                                    property.asFrontMatterValue(value));
                        },
                        () -> dictionary.removeProperty(property.frontMatterProperty()));
            }
        });
    }

    private Optional<?> resolvePropertyValue(Project project, ProjectProperty property)
    {
        for (var resolver : projectPropertyResolverRegistry.resolversFor(property))
        {
            Optional<?> value = resolver.resolveValue(project);
            if (value.isPresent())
            {
                return value;
            }
        }
        return Optional.empty();
    }

    @Override
    public void process(FolderRemoved event)
    {
        if (projectRepository.isProjectFolder(event.folder()))
        {
            projectRepository.projects()
                    .forEach(project -> removeProjectFrontMatter(project.document()));
        }
        super.process(event);
    }

    @Override
    public void process(DocumentRemoved event)
    {
        if (projectRepository.isProjectDocument(event.document()))
        {
            removeProjectFrontMatter(event.document());
        }
        super.process(event);
    }

    @Override
    public void process(ExternalChange event)
    {
        fullRefresh();
    }

    private void removeProjectFrontMatter(Document document)
    {
        frontMatterUpdateCollector.updateFrontMatterFor(document, dictionary -> {
            for (ProjectProperty projectProperty : projectProperties.values())
            {
                dictionary.removeProperty(projectProperty.frontMatterProperty());
            }
        });
    }

    @Override
    public int order()
    {
        return ORDER_LAST;
    }

    public Optional<?> propertyValue(Project project, String propertyName)
    {
        return propertyValue(project, property(propertyName));
    }

    public Optional<?> propertyValue(Project project, ProjectProperty property)
    {
        return Optional.ofNullable(projectPropertyValues.get(project).get(property));
    }
}
