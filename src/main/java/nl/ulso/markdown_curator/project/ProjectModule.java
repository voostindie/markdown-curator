package nl.ulso.markdown_curator.project;

import dagger.*;
import dagger.Module;
import dagger.multibindings.*;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.DataModel;
import nl.ulso.markdown_curator.query.Query;
import nl.ulso.markdown_curator.vault.Document;

import java.time.LocalDate;
import java.util.Map;

import static nl.ulso.markdown_curator.project.AttributeDefinition.*;

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
    abstract DataModel bindProjectRepository(ProjectRepository projectRepository);

    @Binds
    @IntoSet
    abstract DataModel bindFrontMatterAttributeProducer(FrontMatterAttributeProducer producer);

    @Binds
    @IntoSet
    abstract DataModel bindAttributeRegistryModel(AttributeRegistryImpl registry);

    @Binds
    abstract AttributeRegistry bindAttributeRegistry(AttributeRegistryImpl registry);

    @Binds
    @IntoSet
    abstract DataModel bindFrontMatterPropertyWriter(FrontMatterPropertyWriter producer);

    @Binds
    @IntoSet
    abstract Query bindProjectLeadQuery(ProjectLeadQuery query);

    @Binds
    @IntoSet
    abstract Query bindProjectListQuery(ProjectListQuery projectListQuery);
}
