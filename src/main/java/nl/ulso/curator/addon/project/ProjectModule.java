package nl.ulso.curator.addon.project;

import dagger.*;
import dagger.Module;
import dagger.multibindings.*;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.vault.Document;

import java.time.LocalDate;
import java.util.Map;

import static nl.ulso.curator.addon.project.AttributeDefinition.*;

/// Provides a pluggable project model based on a set of documents in a subfolder of the vault.
/// Project data can be extended with metadata from several sources.
///
/// All [Project]s in the vault are made available through the [ProjectRepository].
///
/// Projects have attributes. The set of attributes is configurable, as is the way these attributes
/// are resolved. By default, the following attributes are provided, all resolved from the front
/// matter in the project documents themselves:
///
/// - [#LAST_MODIFIED]: the last modification date of the project, a [java.time.LocalDate].
/// - [#LEAD]: the lead on the project, a [nl.ulso.curator.vault.Document].
/// - [#PRIORITY]: the priority of the project, an [Integer].
/// - [#STATUS]: the status of the project, a [String].
///
/// Project attributes, when resolved, are written back to the front matter of the project
/// documents. (In the default case, where attributes are resolved from these same documents,
/// nothing happens.)
///
/// To add an attribute, [Provides] a [Singleton] [AttributeDefinition] [IntoMap] with a unique
/// [StringKey].
///
/// To resolve attribute values from a different source, implement a [ChangeProcessor] that produces
/// [AttributeValue]s with a specific weight. See [FrontMatterAttributeProducer] for an example (the
/// default).
///
/// This module has one unsatisfied dependency: [ProjectSettings]. It must be provided by the
/// application that imports this module.
@Module
public abstract class ProjectModule
{
    @Multibinds
    abstract Map<String, AttributeDefinition> bindAttributeDefinitions();

    @Provides
    @Singleton
    @IntoMap
    @StringKey(LAST_MODIFIED)
    static AttributeDefinition provideLastModifiedAttribute()
    {
        return newAttributeDefinition(LocalDate.class, LAST_MODIFIED, Object::toString);
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(LEAD)
    static AttributeDefinition provideLeadAttribute()
    {
        return newAttributeDefinition(Document.class, LEAD, d -> ((Document) d).link());
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(PRIORITY)
    static AttributeDefinition providePriorityAttribute()
    {
        return newAttributeDefinition(Integer.class, PRIORITY);
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(STATUS)
    static AttributeDefinition provideStatusAttribute()
    {
        return newAttributeDefinition(String.class, STATUS);
    }

    @Binds
    @IntoSet
    abstract ChangeProcessor bindProjectRepositoryProcessor(
        ProjectRepositoryImpl projectRepository);

    @Binds
    abstract ProjectRepository bindProjectRepository(ProjectRepositoryImpl projectRepository);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindFrontMatterAttributeProducer(
        FrontMatterAttributeProducer producer);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindAttributeRegistryProcessor(AttributeRegistryImpl registry);

    @Binds
    abstract AttributeRegistry bindAttributeRegistry(AttributeRegistryImpl registry);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindFrontMatterPropertyWriter(FrontMatterPropertyWriter producer);

    @Binds
    @IntoSet
    abstract Query bindProjectLeadQuery(ProjectLeadQuery query);

    @Binds
    @IntoSet
    abstract Query bindProjectListQuery(ProjectListQuery projectListQuery);
}
