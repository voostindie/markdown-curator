package nl.ulso.curator.addon.backlink;

/// Represents a backlink.
///
/// Seen as a link, it is a reference from the source document to the target document. By collecting
/// all links for a target document, we have its *back*links.
record Backlink(String targetDocumentName, String sourceDocumentName)
{
}
