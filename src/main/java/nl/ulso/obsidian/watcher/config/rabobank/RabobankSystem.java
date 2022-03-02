package nl.ulso.obsidian.watcher.config.rabobank;

import nl.ulso.obsidian.watcher.System;
import nl.ulso.obsidian.watcher.graph.InMemoryVaultGraph;
import nl.ulso.obsidian.watcher.vault.FileSystemVault;
import nl.ulso.obsidian.watcher.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class RabobankSystem
        implements System
{
    private final Vault vault;
    private final InMemoryVaultGraph graph;

    public RabobankSystem()
            throws IOException
    {
        vault = new FileSystemVault(Path.of("/Users", "vincent", "Notes", "Rabobank"));
        graph = new InMemoryVaultGraph(vault, new RabobankTaxonomy());
        graph.construct();
    }

    public Vault vault()
    {
        return vault;
    }
}
