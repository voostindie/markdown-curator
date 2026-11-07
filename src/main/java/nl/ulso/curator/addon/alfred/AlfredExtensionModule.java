package nl.ulso.curator.addon.alfred;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.extension.ExtensionFactory;

/// Makes Alfred support available as an extension named `alfred`. Include this module in a curator
/// module, and enable the extension in the configuration document, together with the `journal`
/// extension it requires.
///
/// @see AlfredExtensionFactory
@Module(subcomponents = AlfredExtension.class)
public abstract class AlfredExtensionModule
{
    @Binds
    @IntoMap
    @StringKey("alfred")
    abstract ExtensionFactory bindAlfredExtensionFactory(AlfredExtensionFactory factory);
}
