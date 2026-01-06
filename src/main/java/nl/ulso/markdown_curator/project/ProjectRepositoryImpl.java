package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;
import nl.ulso.markdown_curator.vault.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static nl.ulso.markdown_curator.Change.*;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static org.slf4j.LoggerFactory.getLogger;

/// Represents a repository of projects on top of a single folder in the vault, excluding its
/// subfolders.
///
/// This repository only keeps track of _active_ projects. A project is considered active if it is
/// in the main project folder. As soon as a project is archived, typically by moving it to a
/// subfolder, the project is no longer considered active and therefore not tracked by this
/// repository.
@Singleton
final class ProjectRepositoryImpl
    extends ChangeProcessorTemplate
    implements ProjectRepository
{
    private static final Logger LOGGER = getLogger(ProjectRepositoryImpl.class);

    private final Vault vault;
    private final String projectFolderName;
    private final Map<String, Project> projects;

    @Inject
    ProjectRepositoryImpl(Vault vault, ProjectSettings settings)
    {
        this.vault = vault;
        this.projectFolderName = settings.projectFolderName();
        this.projects = new HashMap<>();
        registerChangeHandler(isProjectDocument(), this::handleProjectUpdate);
    }

    @Override
    public Set<Class<?>> producedObjectTypes()
    {
        return Set.of(Project.class);
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return super.isFullRefreshRequired(changelog) ||
               changelog.changes().anyMatch(isProjectFolder().and(isDelete().or(isCreate())));
    }

    Predicate<Change<?>> isProjectDocument()
    {
        return isObjectType(Document.class).and(change ->
            {
                var document = (Document) change.object();
                return isProjectFolder(document.folder());
            }
        );
    }

    private Predicate<Change<?>> isProjectFolder()
    {
        return isObjectType(Folder.class).and(change ->
        {
            var folder = (Folder) change.object();
            return isProjectFolder(folder);
        });
    }

    private boolean isProjectFolder(Folder folder)
    {
        return folder != vault &&
               folder.parent() == vault &&
               folder.name().contentEquals(projectFolderName);
    }

    @Override
    public Collection<Change<?>> fullRefresh()
    {
        projects.clear();
        var changes = createChangeCollection();
        var finder = new ProjectFinder();
        vault.accept(finder);
        finder.projects.forEach(project ->
        {
            projects.put(project.name(), project);
            changes.add(create(project, Project.class));
        });
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Built a project repository of {} projects", projects.size());
        }
        return changes;
    }

    private Collection<Change<?>> handleProjectUpdate(Change<?> change)
    {
        var document = (Document) change.object();
        if (change.kind() == DELETE)
        {
            var project = projects.remove(document.name());
            return List.of(delete(project, Project.class));
        }
        else
        {
            var project = new Project(document);
            var previous = projects.put(project.name(), project);
            if (previous == null)
            {
                return List.of(create(project, Project.class));
            }
            else
            {
                return List.of(update(project, Project.class));
            }
        }
    }

    @Override
    public Map<String, Project> projectsByName()
    {
        return unmodifiableMap(projects);
    }

    @Override
    public Collection<Project> projects()
    {
        return unmodifiableCollection(projects.values());
    }

    @Override
    public Optional<Project> projectFor(Document document)
    {
        return Optional.ofNullable(projects.get(document.name()));
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
