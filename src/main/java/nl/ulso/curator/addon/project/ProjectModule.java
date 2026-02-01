package nl.ulso.curator.addon.project;

import dagger.*;
import dagger.Module;
import dagger.multibindings.*;
import jakarta.inject.Singleton;
import nl.ulso.curator.changelog.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.vault.Document;

import java.time.LocalDate;
import java.util.Map;

import static nl.ulso.curator.addon.project.AttributeDefinition.*;

/// Provides a pluggable project model based on a set of documents in a subfolder of the vault.
/// Project data can be extended with metadata from several sources.
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
        return newAttributeDefinition(LocalDate.class, "last_modified", Object::toString);
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(LEAD)
    static AttributeDefinition provideLeadAttribute()
    {
        return newAttributeDefinition(Document.class, "lead", d -> ((Document) d).link());
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(PRIORITY)
    static AttributeDefinition providePriorityAttribute()
    {
        return newAttributeDefinition(Integer.class, "priority");
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(STATUS)
    static AttributeDefinition provideStatusAttribute()
    {
        return newAttributeDefinition(String.class, "status");
    }

    @Binds
    @IntoSet
    abstract ChangeProcessor bindProjectRepositoryProcessor(ProjectRepositoryImpl projectRepository);

    @Binds
    abstract ProjectRepository bindProjectRepository(ProjectRepositoryImpl projectRepository);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindFrontMatterAttributeProducer(FrontMatterAttributeProducer producer);

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
