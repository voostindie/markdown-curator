package nl.ulso.curator.vault;

import nl.ulso.dictionary.Dictionary;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static nl.ulso.emoji.EmojiStripper.stripEmojisFrom;

/// Represents a Markdown document. A document is broken down in a list of [Fragment]s.
///
/// Every document has at least 1 fragment, which is the document's [FrontMatter] (which might be
/// empty, but always exists).
///
/// A [Document] is an immutable representation of a Markdown document on disk. That means that when
/// changes in the vault are detected, the corresponding [Document]s are replaced with new
/// instances. Therefore, it is not safe to store [Document]s in long-term program state.
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
        this.sortableTitle = stripEmojisFrom(title).trim();
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

    /// Check if the document is in the provided path, or in a subfolder of it.
    public boolean isInPath(String... pathFromRoot)
    {
        requireNonNull(pathFromRoot);
        var pathParts = new ArrayList<>(Arrays.asList(pathFromRoot));
        if (pathParts.isEmpty())
        {
            throw new IllegalArgumentException("pathFromRoot may not be empty");
        }
        var firstPart = pathParts.removeLast();
        var currentFolder = this.folder;
        var found = false;
        while (!currentFolder.isRoot())
        {
            if (currentFolder.name().contentEquals(firstPart))
            {
                found = true;
            }
            currentFolder = currentFolder.parent();
            if (found)
            {
                break;
            }
        }
        if (!found)
        {
            return false;
        }
        while (!pathParts.isEmpty())
        {
            if (currentFolder.isRoot())
            {
                return false;
            }
            var part = pathParts.removeLast();
            if (!currentFolder.name().contentEquals(part))
            {
                return false;
            }
            currentFolder = currentFolder.parent();
        }
        return currentFolder.isRoot();
    }
}
