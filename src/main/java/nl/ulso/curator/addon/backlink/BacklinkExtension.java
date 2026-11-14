package nl.ulso.curator.addon.backlink;

import dagger.Subcomponent;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionModule;
import nl.ulso.curator.extension.ExtensionScope;

/// Backlinks as an extension; see [BacklinkModule].
@ExtensionScope
@Subcomponent(modules = {ExtensionModule.class, BacklinkModule.class})
public interface BacklinkExtension
    extends Extension
{
    @Subcomponent.Factory
    interface Factory
    {
        BacklinkExtension create();
    }
}
