package nl.ulso.markdown_curator.project;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.markdown_curator.DataModel;
import nl.ulso.markdown_curator.query.Query;
import nl.ulso.markdown_curator.vault.Vault;

import static nl.ulso.markdown_curator.project.Attribute.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.Attribute.LEAD;
import static nl.ulso.markdown_curator.project.Attribute.PRIORITY;
import static nl.ulso.markdown_curator.project.Attribute.STATUS;

/**
 * Provides a pluggable project model based on a set of documents in a subfolder of the vault.
 * Project data can be extended with metadata from several sources.
 * <p/>
 * This module has one unsatisfied dependency: {@link ProjectSettings}. It must be provided by the
 * application that imports this module.
 */
@Module
public abstract class ProjectModule
{
    @Binds
    @IntoSet
    abstract DataModel projectRepository(ProjectRepository projectRepository);

    @Binds
    abstract AttributeValueResolverRegistry bindResolverRegistry(
            DefaultAttributeValueResolverRegistry registry);

    @Provides
    @IntoSet
    static AttributeValueResolver<?> leadFrontMatterResolver(Vault vault)
    {
        return new FrontMatterAttributeValueResolver<>(LEAD, "lead", vault);
    }

    @Provides
    @IntoSet
    static AttributeValueResolver<?> lastModifiedFrontMatterResolver(Vault vault)
    {
        return new FrontMatterAttributeValueResolver<>(LAST_MODIFIED, "last-modified", vault);
    }

    @Provides
    @IntoSet
    static AttributeValueResolver<?> priorityFrontMatterResolver(Vault vault)
    {
        return new FrontMatterAttributeValueResolver<>(PRIORITY, "priority", vault);
    }

    @Provides
    @IntoSet
    static AttributeValueResolver<?> statusFrontMatterResolver(Vault vault)
    {
        return new FrontMatterAttributeValueResolver<>(STATUS, "status", vault);
    }

    @Binds
    @IntoSet
    abstract Query projectLeadQuery(ProjectLeadQuery query);

    @Binds
    @IntoSet
    abstract Query bindProjectListQuery(ProjectListQuery projectListQuery);
}
