package nl.ulso.curator.addon.journal;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import nl.ulso.curator.addon.newday.NewDayModule;
import nl.ulso.curator.extension.ExtensionFactory;

/// Makes the journal available as an extension named `journal`. The
/// [nl.ulso.curator.CuratorModule] includes this module; enable the extension in the configuration
/// document.
///
/// The journal needs the [nl.ulso.curator.addon.newday.NewDayAlarm] from the curator; this module
/// includes the [NewDayModule] for that reason.
///
/// @see JournalExtensionFactory
@Module(includes = NewDayModule.class, subcomponents = JournalExtension.class)
public abstract class JournalExtensionModule
{
    @Binds
    @IntoMap
    @StringKey("journal")
    abstract ExtensionFactory bindJournalExtensionFactory(JournalExtensionFactory factory);
}
