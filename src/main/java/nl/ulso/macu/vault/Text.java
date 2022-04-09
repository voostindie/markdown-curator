package nl.ulso.macu.vault;

import java.util.List;
import java.util.Objects;

/**
 * Represents a block of text in a Markdown document. This is the default type of content, meaning
 * that anything not specifically handled differently is considered to be text.
 */
public final class Text
        extends LineContainer
        implements Fragment
{
    Text(List<String> lines)
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
        if (o instanceof Text text)
        {
            return Objects.equals(lines(), text.lines());
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
}
