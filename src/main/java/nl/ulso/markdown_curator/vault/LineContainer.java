package nl.ulso.markdown_curator.vault;

import java.util.List;
import java.util.Optional;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Container for a list of {@link String} lines. On construction a sublist is created by dropping
 * all blank lines at the beginning and the end, effectively trimming the input.
 * <p/>
 * The container also contains references to the document and section it belongs to. This turns
 * the document data structure into an internally cyclic one. I'm not a big fan of that, but it does
 * make for a convenient API.
 */
abstract class LineContainer
    implements Visitable
{
    private final List<String> lines;
    private Document document;
    private Section section;

    LineContainer(List<String> lines)
    {
        this.lines = unmodifiableList(shrink(requireNonNull(lines)));
        this.document = null;
        this.section = null;
    }

    private static List<String> shrink(List<String> lines)
    {
        var from = 0;
        var to = lines.size();
        while (from < to && lines.get(from).isBlank())
        {
            from++;
        }
        while (to > from && lines.get(to - 1).isBlank())
        {
            to--;
        }
        return lines.subList(from, to);
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

    public final List<String> lines()
    {
        return lines;
    }

    public boolean isEmpty()
    {
        return lines.isEmpty();
    }

    public final String content()
    {
        return join(lineSeparator(), lines);
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
