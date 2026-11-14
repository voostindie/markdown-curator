package nl.ulso.curator.addon.backlink;

/// Represents a reference to the [BacklinkQuery] in a specific document that refers to some other
/// document.
///
/// The origin document is the document that holds the [BacklinkQuery]. The backlink document is the
/// document the query refers to. (In most cases, this is the origin document itself.)
record BacklinkQueryReference(String originDocumentName, String backlinkDocumentName)
{
}
