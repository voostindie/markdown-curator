package nl.ulso.curator.addon.backlink;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.ExtensionBinding;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;

/// Keeps track of backlinks to documents, but only for documents they are requested for.
///
/// Keeping all backlinks in memory all the time incurs quite a big hit on memory, which is a waste
/// if these backlinks are not actually used.
///
/// For that reason this module keeps track of backlinks only for those documents that are
/// referenced by the [BacklinkQuery] (also provided by this module).
///
/// These are the bindings of the backlinks extension; see [BacklinkExtension] and
/// [BacklinkExtensionFactory].
@Module
abstract class BacklinkModule
{
    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindBacklinkQueryReferenceProducer(
        BacklinkQueryReferenceProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindBacklinkQueryReferenceProcessor(
        DefaultBacklinkQueryReferenceRepository repository);

    @Binds
    abstract BacklinkQueryReferenceRepository bindBacklinkQueryReferenceRepository(
        DefaultBacklinkQueryReferenceRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindBacklinkQueryReferenceTracker(
        DefaultBacklinkQueryReferenceRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindBacklinkProducer(BacklinkProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindBacklinkInitializer(BacklinkInitializer initializer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindBacklinkExpunger(BacklinkExpunger expunger);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindBacklinkProcessor(DefaultBacklinkRepository repository);

    @Binds
    abstract BacklinkRepository bindBacklinkRepository(DefaultBacklinkRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindBacklinkTracker(DefaultBacklinkRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindBacklinksQuery(BacklinkQuery backlinkQuery);
}
