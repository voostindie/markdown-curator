package nl.ulso.macu.graph;

import nl.ulso.macu.vault.Document;

import java.util.Set;

public interface Taxonomy
{
    Set<VertexDescriptor> describe(Document document);
}
