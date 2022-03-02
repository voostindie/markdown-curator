package nl.ulso.obsidian.watcher.config.rabobank;

import nl.ulso.obsidian.watcher.graph.DocumentDescriptor;
import nl.ulso.obsidian.watcher.graph.Taxonomy;
import nl.ulso.obsidian.watcher.vault.Document;
import nl.ulso.obsidian.watcher.vault.Folder;

import java.util.List;
import java.util.Optional;

public class RabobankTaxonomy
        implements Taxonomy
{
    @Override
    public Optional<DocumentDescriptor> describe(List<Folder> path, Document document)
    {
        return Optional.empty();
    }
}
