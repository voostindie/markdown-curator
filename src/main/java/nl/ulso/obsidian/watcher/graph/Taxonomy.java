package nl.ulso.obsidian.watcher.graph;

import nl.ulso.obsidian.watcher.vault.Document;
import nl.ulso.obsidian.watcher.vault.Folder;

import java.util.List;
import java.util.Optional;

public interface Taxonomy
{
    Optional<DocumentDescriptor> describe(List<Folder> path, Document document);
}
