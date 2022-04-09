package nl.ulso.macu.graph;

import nl.ulso.macu.vault.Document;
import nl.ulso.macu.vault.Folder;

import java.util.List;
import java.util.Set;

public interface Taxonomy
{
    Set<VertexDescriptor> describe(List<Folder> path, Document document);
}
