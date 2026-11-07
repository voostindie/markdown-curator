package nl.ulso.curator.addon.journal;

import dagger.BindsInstance;
import dagger.Subcomponent;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionModule;
import nl.ulso.curator.extension.ExtensionScope;

/// The journal as an extension; see [JournalModule].
@ExtensionScope
@Subcomponent(modules = {ExtensionModule.class, JournalModule.class})
public interface JournalExtension
    extends Extension
{
    @Subcomponent.Factory
    interface Factory
    {
        JournalExtension create(@BindsInstance JournalSettings settings);
    }
}
