package nl.ulso.macu.system.music;

import nl.ulso.macu.graph.Taxonomy;
import nl.ulso.macu.graph.VertexDescriptor;
import nl.ulso.macu.vault.Document;

import java.util.Set;

import static java.util.Collections.emptySet;

public class MusicTaxonomy
        implements Taxonomy
{
    @Override
    public Set<VertexDescriptor> describe(Document document)
    {
        return emptySet();
    }
}
