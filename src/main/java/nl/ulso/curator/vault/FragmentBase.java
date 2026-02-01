package nl.ulso.curator.vault;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/// Base class for fragments in a document; it has back-references to the document and optionally
/// the section it is part of.
abstract class FragmentBase
        implements Visitable
{
    private Document document;
    private Section section;

    FragmentBase()
    {
        this.document = null;
        this.section = null;
    }

    final void setInternalReferences(Document document, Section section)
    {
        if (this.document != null)
        {
            throw new AssertionError("Internal references can be set at most once");
        }
        this.document = requireNonNull(document);
        this.section = section;
    }

    public final Document document()
    {
        return document;
    }

    public final Optional<Section> parentSection()
    {
        return Optional.ofNullable(section);
    }
}
