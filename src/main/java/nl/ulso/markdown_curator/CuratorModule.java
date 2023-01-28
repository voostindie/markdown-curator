package nl.ulso.markdown_curator;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.query.builtin.*;
import nl.ulso.markdown_curator.vault.FileSystemVault;
import nl.ulso.markdown_curator.vault.Vault;

import java.nio.file.Path;
import java.util.Locale;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.util.Locale.ENGLISH;

/**
 * Base Guice module for curators. Every new curator must extend this class and register it for
 * the Java {@link java.util.ServiceLoader} API in
 * {@code META-INF/services/nl.ulso.markdown_curator.CuratorModule}.
 * <p/>
 * Curators must do 5 things:
 * <p/>
 * <ol>
 *     <li>Provide a {@link #name()}. This name is used for logging. A single application can
 *     run multiple curators, and each curator uses multiple threads. The curator's name is used
 *     in each of the curator's threads.</li>
 *     <li>Provide a {@link #vaultPath()}. This is the path on disk to the vault (repository)
 *     with Markdown files.</li>
 *     <li>Provide a {@link java.util.Locale}. This will be used for translations in normal
 *     output. (Instructions and error messages are all in English, hard-coded.)</li>
 *     <li>(Optional): register custom {@link DataModel}s. Use either the
 *     {@link #registerDataModel(Class)} or {@link #dataModelBinder()} method for that. Note that
 *     every data model must be a singleton.</li>
 *     <li>(Optional): register custom {@link Query}s. Use either the {@link #registerQuery(Class)}
 *     or {@link #queryBinder()} method for that.</li>
 * </ol>
 * <p/>
 * Curator modules can use the full power of Guice to configure themselves however they want. This
 * base class sets up the base system and some helper methods.
 * <p/>
 * (Note: I might want to split up this module in two independent separate modules later. For now I
 * actually like how I can use a single module to enforce subclasses to provide the required
 * information in the right way.)
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

    /**
     * @return The locale to use for translations in normal output; this defaults to English.
     */
    public Locale locale()
    {
        return ENGLISH;
    }

    @Override
    protected final void configure()
    {
        dataModelBinder = newSetBinder(binder(), DataModel.class);
        queryBinder = newSetBinder(binder(), Query.class);
        bind(Path.class).annotatedWith(VaultPath.class).toInstance(vaultPath());
        bind(Vault.class).to(FileSystemVault.class);
        bind(Locale.class).toInstance(locale());
        bind(DocumentPathResolver.class).to(FileSystemVault.class);
        bind(QueryCatalog.class).to(InMemoryQueryCatalog.class);
        bind(GeneralMessages.class).to(ResourceBundledGeneralMessages.class);
        bind(QueryResultFactory.class);
        bind(Curator.class);
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
        queryBinder().addBinding().to(queryClass);
    }

    protected final void registerDataModel(Class<? extends DataModel> dataModelClass)
    {
        dataModelBinder().addBinding().to(dataModelClass);
    }

    protected final Path pathInUserHome(String... path)
    {
        return Path.of(java.lang.System.getProperty("user.home"), path);
    }
}
