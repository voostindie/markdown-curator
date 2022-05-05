package nl.ulso.macu.vault;

import java.util.*;

/**
 * Represents a block of text in a Markdown document. This is the default type of content, meaning
 * that anything not specifically handled differently is considered to be text.
 */
public final class TextBlock
        extends LineContainer
        implements Fragment
{
    TextBlock(List<String> lines)
    {
        super(lines);
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
            return Objects.equals(lines(), textBlock.lines());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(lines());
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    public List<InternalLink> findInternalLinks()
    {
        var finder = new InternalLinkFinder();
        accept(finder);
        return finder.internalLinks();
    }
}
