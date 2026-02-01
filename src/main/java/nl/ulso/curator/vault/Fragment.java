package nl.ulso.curator.vault;

import java.util.Optional;

/// A fragment is any distinguishable piece of content in a document. For now that is limited
/// to a document's front matter, sections (of any level), and simple text. Every fragment is
/// visitable (see [VaultVisitor].
///
/// Ideally, this is a sealed interface, but this probably requires an update to the EqualsVerifier
/// library, > 3.10.
public interface Fragment
{
    Document document();

    Optional<Section> parentSection();

    void accept(VaultVisitor visitor);
}
