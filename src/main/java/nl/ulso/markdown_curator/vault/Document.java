package nl.ulso.markdown_curator.vault;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static nl.ulso.emoji.EmojiFilter.stripEmojis;

/**
 * Represents a Markdown document. A document is broken down in a list of {@link Fragment}s.
 * <p/>
 * Every document has at least 1 fragment, which is the document's {@link FrontMatter} (which
 * might be empty, but always exists).
 */
public final class Document
        extends FragmentContainer
{
    private Folder folder;
    private final String name;
    private final String title;
    private final String sortableTitle;
    private final long lastModified;

    Document(String name, long lastModified, List<Fragment> fragments)
    {
        super(fragments);
        this.name = name;
        this.title = resolveTitle(name, fragments);
        this.sortableTitle = stripEmojis(title).trim();
        this.lastModified = lastModified;
    }

    void setFolder(Folder folder)
    {
        if (this.folder != null)
        {
            throw new AssertionError("Folder can be set at most once");
        }
        this.folder = requireNonNull(folder);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof Document document)
        {
            return Objects.equals(name, document.name)
                   && Objects.equals(lastModified, document.lastModified)
                   && Objects.equals(fragments(), document.fragments());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, lastModified, fragments());
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

    static Document newDocument(String name, long lastModified, List<String> lines)
    {
        return new DocumentParser(name, lastModified, lines).parse();
    }

    public Folder folder()
    {
        return folder;
    }

    public String name()
    {
        return name;
    }

    public String title()
    {
        return title;
    }

    public String sortableTitle()
    {
        return sortableTitle;
    }

    public long lastModified()
    {
        return lastModified;
    }

    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    public Dictionary frontMatter()
    {
        return ((FrontMatter) fragment(0)).dictionary();
    }

    public List<InternalLink> findInternalLinks()
    {
        var finder = new InternalLinkFinder();
        accept(finder);
        return finder.internalLinks();
    }

    @Override
    public String toString()
    {
        return name;
    }

    public String link()
    {
        return "[[" + name + "]]";
    }
}
