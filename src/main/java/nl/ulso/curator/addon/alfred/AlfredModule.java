package nl.ulso.curator.addon.alfred;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoSet;
import jakarta.inject.Named;
import nl.ulso.curator.addon.projectjournal.ProjectJournalModule;
import nl.ulso.curator.query.Query;

/// Simple module that exposes curator settings for use in the separate Alfred workflow
///
/// See the [Alfred Markdown Curator]()https://github.com/voostindie/alfred-markdown-curator) for
/// more information.
@Module(includes = ProjectJournalModule.class)
public abstract class AlfredModule
{
    @Binds
    @IntoSet
    abstract Query bindAlfredQuery(AlfredQuery query);

    @Binds
    @IntoSet
    abstract Query bindExternalLinksQuery(ExternalLinksQuery query);

    @BindsOptionalOf
    @Named("documentLinkSectionName")
    abstract String optionalDocumentLinkSectionName();
}
