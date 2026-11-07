package nl.ulso.curator.addon.omnifocus;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.ExtensionBinding;
import nl.ulso.curator.extension.ExtensionLifecycle;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;

/// Bindings of the OmniFocus extension, which extends the project extension with functionality
/// related to [OmniFocus](https://www.omnigroup.com/omnifocus).
///
/// This module:
///
/// - Adds a project attribute `priority`, that reflects the priority of the project in OmniFocus.
/// - Adds a project attribute `omnifocus`, that links to the matching project in OmniFocus. The
/// definition of this attribute is contributed by the [OmniFocusExtensionFactory].
/// - Adds a query `omnifocus` that lists the differences between the vault and OmniFocus.
///
/// Both project attributes are published to the project front matter, but they can of course be
/// used elsewhere too.
///
/// Projects are fetched from OmniFocus at startup, and then every few minutes. If you don't see a
/// change in OmniFocus reflected in the vault yet, give it a few minutes!
///
/// This module expects that:
///
/// - All projects for a specific curator live in a folder in OmniFocus. For example, I have 3
/// folders in OmniFocus, one for each area of responsibility, and I have as many curators.
/// - Projects match on name.
///
///
/// @see OmniFocusExtensionFactory
@Module
abstract class OmniFocusModule
{
    @Binds
    abstract OmniFocusRepository bindOmniFocusRepository(DefaultOmniFocusRepository repository);

    @Binds
    abstract OmniFocusMessages bindOmniFocusMessages(ResourceBundleOmniFocusMessages messages);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ExtensionLifecycle bindOmniFocusRepositoryLifecycle(
        DefaultOmniFocusRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindOmniFocusInitializer(OmniFocusInitializer initializer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindOmniFocusAttributeProducer(
        OmniFocusProjectAttributeValueProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindOmniFocusTracker(
        OmniFocusProjectAttributeValueProducer tracker);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindOmniFocusQuery(OmniFocusQuery omniFocusQuery);
}
