package nl.ulso.obsidian.watcher.vault;

import java.util.List;
import java.util.Objects;

import static java.lang.System.lineSeparator;

/**
 * Represents a piece of code in a Markdown document. This is text surrounded with three
 * backticks (```) on separate lines.
 */
public final class CodeBlock
        extends LineContainer
        implements Fragment
{
    private static final int CODE_MARKER_LENGTH = 3; // ```

    public CodeBlock(List<String> lines)
    {
        super(lines);
    }

    public String language()
    {
        return lines().get(0).substring(CODE_MARKER_LENGTH);
    }

    public String code()
    {
        return String.join(lineSeparator(), lines().subList(1, lines().size() - 1));
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof CodeBlock codeBlock)
        {
            return Objects.equals(lines(), codeBlock.lines());
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
