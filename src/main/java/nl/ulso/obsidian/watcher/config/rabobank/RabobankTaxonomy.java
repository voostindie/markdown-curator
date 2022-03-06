package nl.ulso.obsidian.watcher.config.rabobank;

import nl.ulso.obsidian.watcher.graph.Taxonomy;
import nl.ulso.obsidian.watcher.graph.VertexDescriptor;
import nl.ulso.obsidian.watcher.vault.Document;
import nl.ulso.obsidian.watcher.vault.Folder;

import java.util.*;

import static java.util.Collections.emptySet;

public class RabobankTaxonomy
        implements Taxonomy
{
    @Override
    public Set<VertexDescriptor> describe(List<Folder> path, Document document)
    {
        return emptySet();
    }
}
