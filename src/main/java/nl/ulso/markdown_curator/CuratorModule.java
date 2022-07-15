package nl.ulso.markdown_curator;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.query.builtin.*;
import nl.ulso.markdown_curator.vault.FileSystemVault;
import nl.ulso.markdown_curator.vault.Vault;

import java.nio.file.Path;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

/**
 * Base Guice module for curators. Every new curator must extend this class and register it for
 * the Java {@link java.util.ServiceLoader} API (in
 * {@code META-INF/services/nl.ulso.markdown_curator.CuratorModule}).
 * <p/>
 * Curator modules can use the full power of Guice to configure themselves however they want. This
 * base class sets up the base system and some helper methods.
 * <p/>
 * I might want to split up this module in two independent separate modules later. For now I
 * actually like how I can use a single module to enforce subclasses to provide the required
 * information in the right way.
 */
public abstract class CuratorModule
        extends AbstractModule
{
    private Multibinder<Query> queryBinder;
    private Multibinder<DataModel> dataModelBinder;

    /**
     * @return The name of this module; this name is used in the applications logs.
     */
    public abstract String name();

    /**
     * @return The path to the vault on disk.
     * @see #pathInUserHome(String...)
     */
    public abstract Path vaultPath();

    @Override
    protected final void configure()
    {
        dataModelBinder = newSetBinder(binder(), DataModel.class);
        queryBinder = newSetBinder(binder(), Query.class);
        bind(Path.class).annotatedWith(VaultPath.class).toInstance(vaultPath());
        bind(Vault.class).to(FileSystemVault.class);
        bind(DocumentPathResolver.class).to(FileSystemVault.class);
        bind(QueryCatalog.class).to(InMemoryQueryCatalog.class);
        registerDataModel(LinksModel.class);
        registerQuery(BacklinksQuery.class);
        registerQuery(DeadLinksQuery.class);
        registerQuery(ListQuery.class);
        registerQuery(TableQuery.class);
        registerQuery(TableOfContentsQuery.class);
        configureCurator();
    }

    /**
     * Configure this module; this method is called as the last step in the super's
     * {@link #configure()} method.
     */
    protected void configureCurator()
    {
    }

    protected final Multibinder<Query> queryBinder()
    {
        return queryBinder;
    }

    protected final Multibinder<DataModel> dataModelBinder()
    {
        return dataModelBinder;
    }

    protected final void registerQuery(Class<? extends Query> queryClass)
    {
        queryBinder.addBinding().to(queryClass);
    }

    protected final void registerDataModel(Class<? extends DataModel> dataModelClass)
    {
        dataModelBinder.addBinding().to(dataModelClass);
    }

    protected final Path pathInUserHome(String... path)
    {
        return Path.of(java.lang.System.getProperty("user.home"), path);
    }
}
