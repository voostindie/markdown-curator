package nl.ulso.curator.addon.alfred;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.addon.projectjournal.ProjectJournalModule;
import nl.ulso.curator.query.Query;

/// Simple module that exposes curator settings for use in the separate Alfred workflow
///
/// See the [Alfred Markdown Curator]()https://github.com/voostindie/alfred-markdown-curator) for
/// more information.
///
/// This module has an unsatisfied dependency: [ExternalLinksQuerySettings], which must be provided
/// when including this module in another.
@Module(includes = ProjectJournalModule.class)
public abstract class AlfredModule
{
    @Binds
    @IntoSet
    abstract Query bindAlfredQuery(AlfredQuery query);

    @Binds
    @IntoSet
    abstract Query bindExternalLinksQuery(ExternalLinksQuery query);

}
