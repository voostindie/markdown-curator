package nl.ulso.obsidian.watcher.vault;

import java.util.List;

/**
 * Represents a block of text in a Markdown document. This is the default type of content, meaning
 * that anything not specifically handled differently is considered to be text.
 */
public final class Text
        extends LineContainer
        implements Fragment
{
    public Text(List<String> lines)
    {
        super(lines);
    }

    @Override
    public void accept(Visitor visitor)
    {
        visitor.visitText(this);
    }
}
