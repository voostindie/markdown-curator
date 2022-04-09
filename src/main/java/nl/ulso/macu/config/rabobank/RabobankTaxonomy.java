package nl.ulso.macu.config.rabobank;

import nl.ulso.macu.graph.Taxonomy;
import nl.ulso.macu.graph.VertexDescriptor;
import nl.ulso.macu.vault.Document;
import nl.ulso.macu.vault.Folder;

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
