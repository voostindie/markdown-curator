package nl.ulso.macu.vault;

import java.util.*;

import static java.util.Collections.emptyList;

/**
 * Parses a list of {@link String}s into a {@link Document}. This is <strong>not</strong> a full
 * parser for Markdown, nor is it ever intended to be. Markdown documents are only broken down
 * in high-level parts (or: fragments). The contents of the fragments are still plain text
 * (Markdown).
 * <p/>
 * The parser is a bit complicated because the document is turned into a tree of sections of
 * varying levels. Because the document object model is immutable, a section can only a constructed
 * after all its content has been parsed, recursively.
 */
final class DocumentParser
{
    private final String name;
    private final List<String> lines;
    private final Map<Integer, List<Fragment>> fragments;
    private final Stack<MarkdownTokenizer.HeaderLineToken> headers;

    public DocumentParser(String name, List<String> lines)
    {
        this.name = name;
        this.lines = lines;
        fragments = new HashMap<>();
        headers = new Stack<>();
    }

    Document parse()
    {
        fragments.clear();
        fragments.put(0, new ArrayList<>());
        headers.clear();
        var level = 0;
        var frontMatterEnd = -1;
        var fragmentStart = -1;
        MarkdownTokenizer.TokenType fragmentType = null;
        for (var token : new MarkdownTokenizer(lines))
        {
            var type = token.tokenType();
            if (type == MarkdownTokenizer.TokenType.FRONT_MATTER)
            {
                frontMatterEnd = token.lineIndex() + 1;
                continue;
            }
            if (frontMatterEnd != -1)
            {
                fragments.get(0).add(new FrontMatter(lines.subList(0, frontMatterEnd)));
                frontMatterEnd = -1;
            }
            if (type == fragmentType)
            {
                continue;
            }
            if (fragmentStart != -1)
            {
                var fragment = createFragment(fragmentType, fragmentStart, token.lineIndex());
                fragments.get(level).add(fragment);
                fragmentStart = -1;
                fragmentType = null;
            }
            if (type == MarkdownTokenizer.TokenType.TEXT || type == MarkdownTokenizer.TokenType.CODE || type == MarkdownTokenizer.TokenType.QUERY)
            {
                fragmentStart = token.lineIndex();
                fragmentType = type;
                continue;
            }
            if (type == MarkdownTokenizer.TokenType.HEADER)
            {
                var header = (MarkdownTokenizer.HeaderLineToken) token;
                while (header.level() <= level)
                {
                    level = processSection(header.lineIndex());
                }
                level = header.level();
                fragments.put(level, new ArrayList<>());
                headers.push(header);
            }
            if (type == MarkdownTokenizer.TokenType.END_OF_DOCUMENT)
            {
                while (!headers.isEmpty())
                {
                    processSection(token.lineIndex());
                }
                ensureFrontMatterIsPresent();
            }
        }
        return new Document(name, lines, fragments.get(0));
    }

    private Fragment createFragment(MarkdownTokenizer.TokenType type, int start, int end)
    {
        return switch (type)
                {
                    case TEXT -> new Text(lines.subList(start, end));
                    case CODE -> new CodeBlock(lines.subList(start, end));
                    case QUERY -> new Query(lines.subList(start, end));
                    default -> throw new IllegalStateException("Unsupported type " + type);
                };
    }

    private int processSection(int endLineIndex)
    {
        var header = headers.pop();
        var previousLevel = headers.isEmpty() ? 0 : headers.peek().level();
        fragments.get(previousLevel).add(new Section(header.level(), header.title(),
                lines.subList(header.lineIndex(), endLineIndex),
                fragments.get(header.level())));
        return previousLevel;
    }

    private void ensureFrontMatterIsPresent()
    {
        var toplevel = fragments.get(0);
        if (toplevel.isEmpty() || !(toplevel.get(0) instanceof FrontMatter))
        {
            toplevel.add(0, new FrontMatter(emptyList()));
        }
    }
}