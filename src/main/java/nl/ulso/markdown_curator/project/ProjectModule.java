package nl.ulso.markdown_curator.project;

import dagger.*;
import dagger.Module;
import dagger.multibindings.*;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.DataModel;
import nl.ulso.markdown_curator.query.Query;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.Vault;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static nl.ulso.markdown_curator.project.ProjectProperty.*;

/// Provides a pluggable project model based on a set of documents in a subfolder of the vault.
/// Project data can be extended with metadata from several sources.
///
/// This module has one unsatisfied dependency: [ProjectSettings]. It must be provided by the
/// application that imports this module.
@Module
public abstract class ProjectModule
{
    @Multibinds
    abstract Map<String, ProjectProperty> bindProjectProperties();

    @Multibinds
    abstract Set<ValueResolver> bindProjectPropertyResolvers();

    @Binds
    @IntoSet
    abstract DataModel bindProjectRepository(ProjectRepository projectRepository);

    @Binds
    @IntoSet
    abstract DataModel bindProjectPropertyRepository(
        ProjectPropertyRepository projectPropertyRepository);

    @Binds
    abstract ValueResolverRegistry bindResolverRegistry(
        ValueResolverRegistryImpl registry);

    @Provides
    @Singleton
    @IntoMap
    @StringKey(LAST_MODIFIED)
    static ProjectProperty provideLastModifiedProperty()
    {
        return newProperty(LocalDate.class, "last_modified", Object::toString);
    }

    @Provides
    @IntoSet
    static ValueResolver provideLastModifiedFrontMatterResolver(
        Map<String, ProjectProperty> properties, Vault vault)
    {
        return new FrontMatterValueResolver(properties.get(LAST_MODIFIED), vault);
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(LEAD)
    static ProjectProperty provideLeadProperty()
    {
        return newProperty(Document.class, "lead", d -> ((Document) d).link());
    }

    @Provides
    @IntoSet
    static ValueResolver provideLeadFrontMatterResolver(
        Map<String, ProjectProperty> properties, Vault vault)
    {
        return new FrontMatterValueResolver(properties.get(LEAD), vault);
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(PRIORITY)
    static ProjectProperty providePriorityProperty()
    {
        return newProperty(Integer.class, "priority");
    }

    @Provides
    @IntoSet
    static ValueResolver providePriorityFrontMatterResolver(
        Map<String, ProjectProperty> properties, Vault vault)
    {
        return new FrontMatterValueResolver(properties.get(PRIORITY), vault);
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey(STATUS)
    static ProjectProperty provideStatusProperty()
    {
        return newProperty(String.class, "status");
    }

    @Provides
    @IntoSet
    static ValueResolver provideStatusFrontMatterResolver(
        Map<String, ProjectProperty> properties, Vault vault)
    {
        return new FrontMatterValueResolver(properties.get(STATUS), vault);
    }

    @Binds
    @IntoSet
    abstract Query bindProjectLeadQuery(ProjectLeadQuery query);

    @Binds
    @IntoSet
    abstract Query bindProjectListQuery(ProjectListQuery projectListQuery);
}
