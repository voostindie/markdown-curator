package nl.ulso.curator.addon.alfred;

import java.util.Set;

/// TODO: the journal marker names should really be pulled from the vault itself.
public record ExternalLinksQuerySettings(
    String documentSectionName,
    Set<String> journalMarkerNames)
{
}
