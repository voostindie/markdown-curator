package nl.ulso.obsidian.watcher.vault;

import nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.HeaderLineToken;

import java.util.*;

import static java.util.Collections.emptyList;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.END_OF_DOCUMENT;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.FRONT_MATTER;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.HEADER;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.TEXT;

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
        var textStart = -1;
        for (var token : new SimpleMarkdownTokenizer(lines))
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
            if (type == TEXT)
            {
                if (textStart == -1)
                {
                    textStart = token.lineIndex();
                }
                continue;
            }
            if (textStart != -1)
            {
                var text = new Text(lines.subList(textStart, token.lineIndex()));
                if (!text.isEmpty())
                {
                    fragments.get(level).add(text);
                }
                textStart = -1;
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
