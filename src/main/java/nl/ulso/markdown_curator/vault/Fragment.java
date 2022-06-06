package nl.ulso.markdown_curator.vault;

import java.util.List;
import java.util.Optional;

/**
 * A fragment is any distinguishable piece of content in a document. For now that is limited
 * to a document's front matter, sections (of any level), and simple text. Every fragment is
 * visitable (see {@link VaultVisitor}.
 * <p>
 * Ideally this is sealed interface, but this probably requires an update to the EqualsVerifier
 * library, > 3.10.
 */
public interface Fragment
{
    Document document();

    Optional<Section> parentSection();

    List<String> lines();

    String content();

    boolean isEmpty();

    void accept(VaultVisitor visitor);
}
