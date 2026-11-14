package nl.ulso.curator.addon.projectjournal;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.extension.ExtensionFactory;

/// Makes the project journal available as an extension named `projectjournal`. The
/// [nl.ulso.curator.CuratorModule] includes this module; enable the extension in the configuration
/// document, together with the `project` and `journal` extensions it requires.
///
/// @see ProjectJournalExtensionFactory
@Module(subcomponents = ProjectJournalExtension.class)
public abstract class ProjectJournalExtensionModule
{
    @Binds
    @IntoMap
    @StringKey("projectjournal")
    abstract ExtensionFactory bindProjectJournalExtensionFactory(
        ProjectJournalExtensionFactory factory);
}
