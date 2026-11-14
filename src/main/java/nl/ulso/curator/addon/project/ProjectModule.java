package nl.ulso.curator.addon.project;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.ExtensionBinding;
import nl.ulso.curator.extension.ExtensionScope;
import nl.ulso.curator.extension.ServiceRegistration;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.dictionary.Dictionary;

import static nl.ulso.curator.extension.ServiceRegistration.service;

/// Bindings of the project extension; see [ProjectExtension] and [ProjectExtensionFactory].
///
/// Provides a pluggable project model based on a set of documents in a subfolder of the vault.
/// Project data can be extended with metadata from several sources.
///
/// All [Project]s in the vault are made available through the [ProjectRepository]. Projects have
/// attributes, made available through the [ProjectAttributeRepository]. Both are published as
/// services for other extensions to require.
///
/// The set of attributes is an extension point: every [ProjectAttributeDefinition] contributed by
/// the curator or by any extension is available. The project extension itself contributes the
/// default definitions; see [ProjectExtensionFactory].
///
/// To resolve attribute values from a different source, implement a [ChangeProcessor] that produces
/// [ProjectAttributeValue]s with a specific weight. See [FrontMatterProjectAttributeValueProducer]
/// for an example (the default).
@Module
abstract class ProjectModule
{
    @Provides
    @ExtensionScope
    static ProjectSettings provideProjectSettings(Dictionary configuration)
    {
        return ProjectSettings.from(configuration);
    }

    @Binds
    abstract ProjectMessages bindProjectMessages(ResourceBundleProjectMessages messages);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindProjectProducer(ProjectProducer projectProducer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindProjectProcessor(DefaultProjectRepository projectRepository);

    @Binds
    abstract ProjectRepository bindProjectRepository(DefaultProjectRepository projectRepository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindProjectMeasurements(DefaultProjectRepository projectRepository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindFrontMatterAttributeProducer(
        FrontMatterProjectAttributeValueProducer producer);

    @Binds
    abstract ProjectAttributeRepository bindProjectAttributeRepository(
        DefaultProjectAttributeRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindProjectAttributeProcessor(
        DefaultProjectAttributeRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract MeasurementTracker bindProjectAttributeTracker(
        DefaultProjectAttributeRepository repository);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract ChangeProcessor bindFrontMatterPropertyWriter(
        ProjectAttributeValueFrontMatterWriter producer);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindProjectLeadQuery(ProjectLeadQuery query);

    @Binds
    @IntoSet
    @ExtensionBinding
    abstract Query bindProjectListQuery(ProjectListQuery projectListQuery);

    @Provides
    @IntoSet
    @ExtensionBinding
    static ServiceRegistration publishProjectRepository(ProjectRepository repository)
    {
        return service(ProjectRepository.class, repository);
    }

    @Provides
    @IntoSet
    @ExtensionBinding
    static ServiceRegistration publishProjectAttributeRepository(
        ProjectAttributeRepository repository)
    {
        return service(ProjectAttributeRepository.class, repository);
    }
}
