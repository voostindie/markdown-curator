package nl.ulso.curator.addon.alfred;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.query.Query;

/// Simple module that exposes curator settings for use in the separate Alfred workflow
///
/// See <https://github.com/voostindie/alfred-markdown-curator>
///
/// This module has an unsatisfied dependency: [AlfredSettings]. When including this module, also
/// provide an instance of this class.
@Module
public abstract class AlfredModule
{
    @Binds
    @IntoSet
    abstract Query bindAlfredQuery(AlfredQuery alfredQuery);
}
