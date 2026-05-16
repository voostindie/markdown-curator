package nl.ulso.curator.addon.projectjournal;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.addon.journal.JournalModule;
import nl.ulso.curator.addon.project.ProjectModule;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.statistics.MeasurementTracker;

/// Combines the Project and Journal modules by offering additional functionality to extract project
/// attributes from the journal.
///
/// This module adds resolvers for the project lead, status, and last modification that have
/// greater weights than the resolvers in the Project module that use front matter.
///
/// An earlier, rudimentary implementation of this module pulled attribute values for all supported
/// attribute definitions from the journal. That was efficient but hard to maintain. This
/// implementation has a more modular approach: every attribute pulled from the journal is coded
/// separately.
@Module(includes = {
    JournalModule.class,
    ProjectModule.class
})
public abstract class ProjectJournalModule
{
    @Binds
    @IntoSet
    abstract ChangeProcessor bindProjectLeadMarkerProcessor(
        ProjectLeadMarkerRepository
            processor);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindProjectLeadMarkerTracker(
        ProjectLeadMarkerRepository
            tracker);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindLeadProjectAttributeValueProducer(
        ProjectLeadAttributeValueProducer producer);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindLeadProjectAttributeValueTracker(
        ProjectLeadAttributeValueProducer producer);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindProjectStatusMarkerProcessor(
        ProjectStatusMarkerRepository processor);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindProjectStatusMarkerTracker(
        ProjectStatusMarkerRepository tracker);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindStatusProjectAttributeValueProducer(
        ProjectStatusAttributeValueProducer producer);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindStatusProjectAttributeValueTracker(
        ProjectStatusAttributeValueProducer producer);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindLastModifiedProjectAttributeValueProducer(
        ProjectLastModifiedAttributeValueProducer producer);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindLastModifiedProjectAttributeValueTracker(
        ProjectLastModifiedAttributeValueProducer producer);
}
