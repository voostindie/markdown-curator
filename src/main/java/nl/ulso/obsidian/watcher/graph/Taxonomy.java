package nl.ulso.obsidian.watcher.graph;

import nl.ulso.obsidian.watcher.vault.Document;
import nl.ulso.obsidian.watcher.vault.Folder;

import java.util.List;
import java.util.Set;

public interface Taxonomy
{
    Set<VertexDescriptor> describe(List<Folder> path, Document document);
}
