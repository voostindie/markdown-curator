package nl.ulso.markdown_curator.vault;

import java.util.List;

/**
 * A fragment is any distinguishable piece of content in a document. For now that is limited
 * to a document's front matter, sections (of any level), and simple text. Every fragment is
 * visitable (see {@link VaultVisitor}.
 * <p>
 * TODO: make this is sealed interface. This requires an update to the EqualsVerifier, > 3.9.
 */
public interface Fragment
{
    Document document();

    List<String> lines();

    String content();

    boolean isEmpty();

    void accept(VaultVisitor visitor);
}