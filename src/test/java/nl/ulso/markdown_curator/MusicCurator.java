package nl.ulso.markdown_curator;

import dagger.Component;
import nl.ulso.markdown_curator.query.QueryCatalog;
import nl.ulso.markdown_curator.vault.Vault;

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
