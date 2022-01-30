package nl.ulso.obsidian.watcher.vault;

import java.util.List;

/**
 * Represents a Markdown document. A document is broken down in a list of {@link Fragment}s.
 * <p/>
 * Every document has at least 1 fragment, which is the document's {@link FrontMatter} (which
 * might be empty, but always exists).
 * <p/>
 * A document is constructed from a list of {@link String} lines. These lines are kept, next to
 * the fragments Each fragment in turn gives access to the underlying lines that it captures.
 */
public final class Document
        extends FragmentContainer
{
    private final String name;
    private final String title;

    Document(String name, List<String> lines, List<Fragment> fragments)
    {
        super(lines, fragments);
        this.name = name;
        this.title = resolveTitle(name, fragments);
    }

    private String resolveTitle(String name, List<Fragment> fragments)
    {
        if (fragments.size() > 1
                && fragments.get(1) instanceof Section section
                && section.level() == 1)
        {
            return section.title();
        }
        var frontMatter = (FrontMatter) fragments.get(0);
        return frontMatter.dictionary().string("title", name);
    }

    static Document newDocument(String name, List<String> lines)
    {
        return new DocumentParser(name, lines).parse();
    }

    public String name()
    {
        return name;
    }

    public String title()
    {
        return title;
    }

    public void accept(Visitor visitor)
    {
        visitor.visitDocument(this);
    }

    public Dictionary frontMatter()
    {
        return ((FrontMatter) fragment(0)).dictionary();
    }
}
