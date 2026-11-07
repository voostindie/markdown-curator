package nl.ulso.curator.addon.alfred;

import dagger.BindsInstance;
import dagger.Subcomponent;
import jakarta.inject.Named;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionModule;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.Optional;

/// Alfred support as an extension; see [AlfredModule].
@ExtensionScope
@Subcomponent(modules = {ExtensionModule.class, AlfredModule.class})
public interface AlfredExtension
    extends Extension
{
    @Subcomponent.Factory
    interface Factory
    {
        AlfredExtension create(
            @BindsInstance AlfredSettings settings,
            @BindsInstance Journal journal,
            @BindsInstance @Named("documentLinkSectionName")
            Optional<String> documentLinkSectionName);
    }
}
