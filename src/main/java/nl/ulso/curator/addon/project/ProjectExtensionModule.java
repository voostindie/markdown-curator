package nl.ulso.curator.addon.project;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.extension.ExtensionFactory;

/// Makes the project model available as an extension named `project`. The
/// [nl.ulso.curator.CuratorModule] includes this module; enable the extension in the configuration
/// document.
///
/// @see ProjectExtensionFactory
@Module(subcomponents = ProjectExtension.class)
public abstract class ProjectExtensionModule
{
    @Binds
    @IntoMap
    @StringKey("project")
    abstract ExtensionFactory bindProjectExtensionFactory(ProjectExtensionFactory factory);
}
