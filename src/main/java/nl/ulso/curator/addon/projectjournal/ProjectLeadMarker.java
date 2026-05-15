package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.journal.Marker;
import nl.ulso.curator.vault.Vault;

import java.util.Optional;

import static nl.ulso.curator.vault.InternalLinkFinder.extractInternalLinksFrom;

/// [ProjectMarker] that is used to infer project leads from the journal. If the project marker is
/// present in the journal with an alias as defined in the `project-leads` front matter property,
/// the project lead is inferred from the first valid link to a document in the vault.
final class ProjectLeadMarker
    extends ProjectMarker
{
    private static final String PROJECT_LEADS_MARKER_PROPERTY = "project-leads";

    ProjectLeadMarker(Marker marker)
    {
        super(marker);
    }

    public static boolean isProjectLeadMarker(Marker marker)
    {
        return marker.document().frontMatter().hasProperty(PROJECT_LEADS_MARKER_PROPERTY);
    }

    @Override
    String frontMatterProperty()
    {
        return PROJECT_LEADS_MARKER_PROPERTY;
    }

    /// The project lead is the first valid internal document link on the line that doesn't link to
    /// this marker itself. (That document doesn't have to link to a "person", but that's up to the
    /// journal author.)
    Optional<Object> resolveLeadFrom(String journalEntryLine, Vault vault)
    {
        return extractInternalLinksFrom(journalEntryLine).stream()
            .filter(link -> !link.targetDocument().contentEquals(name()))
            .map(link -> vault.findDocument(link.targetDocument()))
            .flatMap(Optional::stream)
            .map(document -> (Object) document)
            .findFirst();
    }
}
