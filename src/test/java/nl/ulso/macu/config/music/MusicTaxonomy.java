package nl.ulso.macu.config.music;

import nl.ulso.macu.graph.Taxonomy;
import nl.ulso.macu.graph.VertexDescriptor;
import nl.ulso.macu.vault.Document;
import nl.ulso.macu.vault.Folder;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

public class MusicTaxonomy
        implements Taxonomy
{
    @Override
    public Set<VertexDescriptor> describe(List<Folder> path, Document document)
    {
        return emptySet();
    }
}
