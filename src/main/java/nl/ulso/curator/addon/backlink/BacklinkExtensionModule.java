package nl.ulso.curator.addon.backlink;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.extension.ExtensionFactory;

/// Makes backlinks available as an extension named `backlinks`. The
/// [nl.ulso.curator.CuratorModule] includes this module; enable the extension in the configuration
/// document.
///
/// @see BacklinkExtensionFactory
@Module(subcomponents = BacklinkExtension.class)
public abstract class BacklinkExtensionModule
{
    @Binds
    @IntoMap
    @StringKey("backlinks")
    abstract ExtensionFactory bindBacklinkExtensionFactory(BacklinkExtensionFactory factory);
}
