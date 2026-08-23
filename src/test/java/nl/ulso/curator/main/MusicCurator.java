package nl.ulso.curator.main;

import dagger.Component;
import jakarta.inject.Singleton;
import nl.ulso.curator.Curator;
import nl.ulso.curator.query.QueryCatalog;
import nl.ulso.curator.query.QueryResultFormatterRegistry;
import nl.ulso.curator.vault.DocumentPathResolver;
import nl.ulso.curator.vault.Vault;

@Singleton
@Component(modules = MusicCuratorModule.class)
interface MusicCurator
{
    Curator curator();

    Vault vault();

    QueryCatalog queryCatalog();

    QueryResultFormatterRegistry queryResultFormatterRegistry();

    QueryOrchestrator queryOrchestrator();

    DocumentPathResolver documentPathResolver();
}
