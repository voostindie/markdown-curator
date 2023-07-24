package nl.ulso.markdown_curator.vault;

import java.util.List;
import java.util.Objects;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

/**
 * Represents a piece of code in a Markdown document. This is text surrounded with three
 * backticks (```) on separate lines.
 */
public final class CodeBlock
        extends FragmentBase
        implements Fragment
{
    static final String CODE_MARKER = "```";

    private final String language;
    private final String code;

    CodeBlock(List<String> lines)
    {
        language = lines.get(0).substring(CODE_MARKER.length());
        code = join(lineSeparator(), lines.subList(1, lines.size() - 1));
    }

    public String markdown()
    {
        return CODE_MARKER + language + lineSeparator() +
               code + lineSeparator() +
               CODE_MARKER + lineSeparator();
    }

    public String language()
    {
        return language;
    }

    public String code()
    {
        return code;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof CodeBlock codeBlock)
        {
            return Objects.equals(language, codeBlock.language)
                   && Objects.equals(code, codeBlock.code);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(language, code);
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }
}
