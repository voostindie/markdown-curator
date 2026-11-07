package nl.ulso.curator.addon.projectjournal;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.ExtensionBinding;
import nl.ulso.curator.statistics.MeasurementTracker;

/// Combines the Project and Journal modules by offering additional functionality to extract project
/// attributes from the journal.
///
/// This module adds resolvers for the project lead, status, and last modification that have greater
/// weights than the resolvers in the Project module that use front matter.
///
/// An earlier, rudimentary implementation of this module pulled attribute values for all supported
/// attribute definitions from the journal. That was efficient but hard to maintain. This
/// implementation has a more modular approach: every attribute pulled from the journal is coded
/// separately.
///
/// These are the bindings of the project journal extension; see [ProjectJournalExtension] and
/// [ProjectJournalExtensionFactory].
@Module
abstract class ProjectJournalModule
{
    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindProjectLeadMarkerProducer(ProjectLeadMarkerProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindProjectLeadMarkerProcessor(ProjectLeadMarkerRepository processor);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindProjectLeadMarkerTracker(
        ProjectLeadMarkerRepository tracker);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindLeadProjectAttributeValueProducer(
        ProjectLeadAttributeValueProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindLeadProjectAttributeValueTracker(
        ProjectLeadAttributeValueProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindProjectStatusMarkerProducer(ProjectStatusMarkerProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindProjectStatusMarkerProcessor(
        ProjectStatusMarkerRepository processor);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindProjectStatusMarkerTracker(
        ProjectStatusMarkerRepository tracker);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindStatusProjectAttributeValueProducer(
        ProjectStatusAttributeValueProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindStatusProjectAttributeValueTracker(
        ProjectStatusAttributeValueProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindLastModifiedProjectAttributeValueProducer(
        ProjectLastModifiedAttributeValueProducer producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindLastModifiedProjectAttributeValueTracker(
        ProjectLastModifiedAttributeValueProducer producer);
}
