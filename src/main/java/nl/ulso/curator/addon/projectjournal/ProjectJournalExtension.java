package nl.ulso.curator.addon.projectjournal;

import dagger.BindsInstance;
import dagger.Subcomponent;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionModule;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.Map;

/// The project journal as an extension; see [ProjectJournalModule].
///
/// The services it requires from the project and journal extensions are passed into the factory,
/// so that the classes in this extension can inject them as usual.
@ExtensionScope
@Subcomponent(modules = {ExtensionModule.class, ProjectJournalModule.class})
public interface ProjectJournalExtension
    extends Extension
{
    @Subcomponent.Factory
    interface Factory
    {
        ProjectJournalExtension create(
            @BindsInstance ProjectRepository projectRepository,
            @BindsInstance Journal journal,
            @BindsInstance Map<String, ProjectAttributeDefinition> attributeDefinitions);
    }
}
