package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.EntityTransformer;
import nl.ulso.curator.vault.Document;

import java.util.Optional;

/// This producer only keeps track of _active_ projects. A project is considered active if it is
/// in the main project folder. As soon as a project is archived, typically by moving it to a
/// subfolder, the project is no longer considered active and therefore not tracked by this
/// repository.
///
/// For each project that is created, updated, or deleted, this repository publishes a change with
/// the relevant [Project] as its payload.
@Singleton
final class ProjectProducer
    extends EntityTransformer<Document, Project>
{
    private final String projectFolderName;

    @Inject
    ProjectProducer(ProjectSettings settings)
    {
        this.projectFolderName = settings.projectFolderName();
    }

    @Override
    protected Class<Document> sourceClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Project> targetClass()
    {
        return Project.class;
    }

    @Override
    protected Optional<Project> transform(Document document)
    {
        if (!document.folder().name().contentEquals(projectFolderName) ||
            document.folder().isRoot() || !document.folder().parent().isRoot())
        {
            return Optional.empty();
        }
        return Optional.of(new Project(document));
    }
}
