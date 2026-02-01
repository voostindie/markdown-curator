package nl.ulso.curator.vault;

import java.util.*;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

/**
 * Represents a block of text in a Markdown document. This is the default type of content, meaning
 * that anything not specifically handled differently is considered to be text.
 */
public final class TextBlock
        extends FragmentBase
        implements Fragment
{
    private final String markdown;

    TextBlock(List<String> lines)
    {
        this.markdown = join(lineSeparator(), lines) + lineSeparator();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof TextBlock textBlock)
        {
            return Objects.equals(markdown, textBlock.markdown);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(markdown);
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    public String markdown()
    {
        return markdown;
    }

    public List<InternalLink> findInternalLinks()
    {
        var finder = new InternalLinkFinder();
        accept(finder);
        return finder.internalLinks();
    }
}
