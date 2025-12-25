package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
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
    public void fullRefresh()
    {
        projects.clear();
        var finder = new ProjectFinder();
        vault.accept(finder);
        finder.projects.forEach(project -> projects.put(project.name(), project));
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Built a project repository of {} projects", projects.size());
        }
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
    public void process(FolderAdded event)
    {
        processFolderEventFor(event.folder());
    }

    @Override
    public void process(FolderRemoved event)
    {
        processFolderEventFor(event.folder());
    }

    private void processFolderEventFor(Folder folder)
    {
        if (isProjectFolder(folder))
        {
            fullRefresh();
        }
    }

    @Override
    public void process(DocumentAdded event)
    {
        processDocumentEventFor(event.document());
    }

    @Override
    public void process(DocumentChanged event)
    {
        processDocumentEventFor(event.document());
    }

    private void processDocumentEventFor(Document document)
    {
        if (isProjectDocument(document))
        {
            var project = new Project(document);
            projects.put(project.name(), new Project(document));
        }
    }

    @Override
    public void process(DocumentRemoved event)
    {
        if (isProjectDocument(event.document()))
        {
            projects.remove(event.document().name());
        }
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
