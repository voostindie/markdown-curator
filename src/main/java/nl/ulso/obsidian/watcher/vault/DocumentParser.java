package nl.ulso.obsidian.watcher.vault;

import nl.ulso.obsidian.watcher.vault.MarkdownTokenizer.HeaderLineToken;
import nl.ulso.obsidian.watcher.vault.MarkdownTokenizer.TokenType;

import java.util.*;

import static java.util.Collections.emptyList;
import static nl.ulso.obsidian.watcher.vault.MarkdownTokenizer.TokenType.*;

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
    private final Stack<HeaderLineToken> headers;

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
        var queryDefinitionStart = -1;
        var queryResultStart = -1;
        TokenType fragmentType = null;
        for (var token : new MarkdownTokenizer(lines))
        {
            var type = token.tokenType();
            if (type == FRONT_MATTER)
            {
                frontMatterEnd = token.lineIndex() + 1;
                continue;
            }
            if (frontMatterEnd != -1)
            {
                fragments.get(0).add(new FrontMatter(lines.subList(0, frontMatterEnd)));
                frontMatterEnd = -1;
            }
            if (type == QUERY_DEFINITION)
            {
                if (queryDefinitionStart == -1)
                {
                    queryDefinitionStart = token.lineIndex();
                }
                continue;
            }
            if (type == QUERY_RESULT)
            {
                if (queryResultStart == -1)
                {
                    queryResultStart = token.lineIndex();
                }
                continue;
            }
            if (type == QUERY_END)
            {
                var query = new Query(lines.subList(queryDefinitionStart, token.lineIndex() + 1),
                        queryResultStart - queryDefinitionStart);
                fragments.get(level).add(query);
                queryDefinitionStart = -1;
                queryResultStart = -1;
                continue;
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
            if (type == TEXT || type == CODE)
            {
                fragmentStart = token.lineIndex();
                fragmentType = type;
                continue;
            }
            if (type == HEADER)
            {
                var header = (HeaderLineToken) token;
                while (header.level() <= level)
                {
                    level = processSection(header.lineIndex());
                }
                level = header.level();
                fragments.put(level, new ArrayList<>());
                headers.push(header);
            }
            if (type == END_OF_DOCUMENT)
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

    private Fragment createFragment(TokenType type, int start, int end)
    {
        return switch (type)
                {
                    case TEXT -> new Text(lines.subList(start, end));
                    case CODE -> new CodeBlock(lines.subList(start, end));
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
