package nl.ulso.curator.addon.alfred;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.extension.ExtensionBinding;
import nl.ulso.curator.query.Query;
import nl.ulso.dictionary.Dictionary;

/// Bindings of the Alfred extension, which exposes curator settings for use in the separate Alfred
/// workflow; see [AlfredExtension] and [AlfredExtensionFactory].
///
/// See the [Alfred Markdown
/// Curator](https://github.com/voostindie/alfred-markdown-curator-workflow) for more information.
@Module
abstract class AlfredModule
{
    @Provides
    static AlfredSettings provideAlfredSettings(Dictionary configuration)
    {
        return AlfredSettings.from(configuration);
    }

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindAlfredQuery(AlfredQuery query);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindExternalLinksQuery(ExternalLinksQuery query);
}
