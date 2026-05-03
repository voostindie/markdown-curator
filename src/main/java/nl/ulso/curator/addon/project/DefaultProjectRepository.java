package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.MapBasedEntityRepository;
import nl.ulso.curator.vault.Document;

import java.util.*;

/// Represents a repository of projects on top of a single folder in the vault, excluding its
/// subfolders.
///
/// This repository only keeps track of _active_ projects. A project is considered active if it is
/// in the main project folder. As soon as a project is archived, typically by moving it to a
/// subfolder, the project is no longer considered active and therefore not tracked by this
/// repository.
///
/// For each project that is created, updated, or deleted, this repository publishes a change with
/// the relevant [Project] as its payload.
@Singleton
final class DefaultProjectRepository
    extends MapBasedEntityRepository<Document, String, Project>
    implements ProjectRepository
{
    private final String projectFolderName;

    @Inject
    DefaultProjectRepository(ProjectSettings settings)
    {
        this.projectFolderName = settings.projectFolderName();
    }

    @Override
    protected Class<Document> sourceEntityClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Project> targetEntityClass()
    {
        return Project.class;
    }

    @Override
    protected boolean isEntity(Document document)
    {
        return document.folder().name().contentEquals(projectFolderName) &&
               !document.folder().isRoot() && document.folder().parent().isRoot();
    }

    @Override
    protected String entityKeyFrom(Document document)
    {
        return document.name();
    }

    @Override
    protected Project createEntityFrom(String name, Document document)
    {
        return new Project(document);
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
        return Optional.ofNullable(map().get(document.name()));
    }

    @Override
    public String toString()
    {
        return ProjectRepository.class.getSimpleName();
    }
}
