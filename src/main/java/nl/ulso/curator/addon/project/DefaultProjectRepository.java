package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.MapBasedEntityRepository;
import nl.ulso.curator.vault.Document;

import java.util.*;

/// Represents a repository of projects on top of a single folder in the vault, excluding its
/// subfolders.
@Singleton
final class DefaultProjectRepository
    extends MapBasedEntityRepository<String, Project>
    implements ProjectRepository
{
    @Inject
    DefaultProjectRepository()
    {
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return ProjectRepository.class;
    }

    @Override
    protected Class<Project> entityClass()
    {
        return Project.class;
    }

    @Override
    protected String entityKeyFrom(Project project)
    {
        return project.name();
    }

    @Override
    public Map<String, Project> projectsByName()
    {
        return map();
    }

    @Override
    public Collection<Project> projects()
    {
        return map().values();
    }

    @Override
    public Optional<Project> projectFor(Document document)
    {
        return projectNamed(document.name());
    }

    @Override
    public Optional<Project> projectNamed(String name)
    {
        return Optional.ofNullable(map().get(name));
    }
}
