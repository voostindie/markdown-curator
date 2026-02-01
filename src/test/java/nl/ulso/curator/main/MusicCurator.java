package nl.ulso.curator.main;

import dagger.Component;
import nl.ulso.curator.Curator;
import nl.ulso.curator.query.QueryCatalog;
import nl.ulso.curator.vault.DocumentPathResolver;
import nl.ulso.curator.vault.Vault;

import jakarta.inject.Singleton;

@Singleton
@Component(modules = MusicCuratorModule.class)
interface MusicCurator
{
    Curator curator();

    Vault vault();

    QueryCatalog queryCatalog();

    QueryOrchestrator queryOrchestrator();

    DocumentPathResolver documentPathResolver();
}
