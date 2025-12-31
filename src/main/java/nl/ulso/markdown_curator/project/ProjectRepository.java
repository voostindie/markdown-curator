package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static nl.ulso.markdown_curator.Changelog.emptyChangelog;
import static org.slf4j.LoggerFactory.getLogger;

/// Represents a repository of projects on top of a single folder in the vault, excluding its
/// subfolders.
///
/// This repository only keeps track of _active_ projects. A project is considered active if it is
/// in the main project folder. As soon as a project is archived, typically by moving it to a
/// subfolder, the project is no longer considered active and therefore not tracked by this
/// repository.
@Singleton
public final class ProjectRepository
    extends DataModelTemplate
{
    private static final Logger LOGGER = getLogger(ProjectRepository.class);

    private final Vault vault;
    private final String projectFolderName;
    private final Map<String, Project> projects;

    @Inject
    ProjectRepository(Vault vault, ProjectSettings settings)
    {
        this.vault = vault;
        this.projectFolderName = settings.projectFolderName();
        this.projects = new ConcurrentHashMap<>();
    }

    @Override
    public Changelog fullRefresh(Changelog changelog)
    {
        projects.clear();
        var finder = new ProjectFinder();
        vault.accept(finder);
        finder.projects.forEach(project -> projects.put(project.name(), project));
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Built a project repository of {} projects", projects.size());
        }
        return emptyChangelog();
    }

    public Map<String, Project> projectsByName()
    {
        return unmodifiableMap(projects);
    }

    public Collection<Project> projects()
    {
        return unmodifiableCollection(projects.values());
    }

    public boolean isProjectDocument(Document document)
    {
        return isProjectFolder(document.folder());
    }

    public Optional<Project> projectFor(Document document)
    {
        return Optional.ofNullable(projects.get(document.name()));
    }

    boolean isProjectFolder(Folder folder)
    {
        return folder != vault &&
               folder.parent() == vault &&
               folder.name().contentEquals(projectFolderName);
    }

    @Override
    public Changelog process(FolderAdded event, Changelog changelog)
    {
        return processFolderEventFor(event.folder(), changelog);
    }

    @Override
    public Changelog process(FolderRemoved event, Changelog changelog)
    {
        return processFolderEventFor(event.folder(), changelog);
    }

    private Changelog processFolderEventFor(Folder folder, Changelog changelog)
    {
        if (isProjectFolder(folder))
        {
            return fullRefresh(changelog);
        }
        return emptyChangelog();
    }

    @Override
    public Changelog process(DocumentAdded event, Changelog changelog)
    {
        return processDocumentEventFor(event.document(), changelog);
    }

    @Override
    public Changelog process(DocumentChanged event, Changelog changelog)
    {
        return processDocumentEventFor(event.document(), changelog);
    }

    private Changelog processDocumentEventFor(Document document, Changelog changelog)
    {
        if (isProjectDocument(document))
        {
            var project = new Project(document);
            projects.put(project.name(), new Project(document));
        }
        return emptyChangelog();
    }

    @Override
    public Changelog process(DocumentRemoved event, Changelog changelog)
    {
        if (isProjectDocument(event.document()))
        {
            projects.remove(event.document().name());
        }
        return emptyChangelog();
    }

    private class ProjectFinder
        extends BreadthFirstVaultVisitor
    {
        private final List<Project> projects = new ArrayList<>();

        @Override
        public void visit(Vault vault)
        {
            // Visit only the project folder, not the whole vault, nor the subfolders.
            vault.folder(projectFolderName).ifPresent(
                folder -> folder.documents()
                    .forEach(document -> document.accept(this)));
        }

        @Override
        public void visit(Document document)
        {
            projects.add(new Project(document));
        }
    }
}
