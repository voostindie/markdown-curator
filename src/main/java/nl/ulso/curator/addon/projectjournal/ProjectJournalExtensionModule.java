package nl.ulso.curator.addon.projectjournal;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.extension.ExtensionFactory;

/// Makes the project journal available as an extension named `projectjournal`. Include this module
/// in a curator module, and enable the extension in the configuration document, together with the
/// `project` and `journal` extensions it requires.
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
