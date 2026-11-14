package nl.ulso.curator.addon.backlink;

import java.util.Set;

interface BacklinkQueryReferenceRepository
{
    boolean hasBacklinkReference(String backlinkDocumentName);

    /// Returns the set of document names for which backlinks must be processed.
    Set<String> backlinkDocumentNames();
}
