package nl.ulso.markdown_curator.vault;

import java.util.*;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.vault.CodeBlock.CODE_MARKER;
import static nl.ulso.markdown_curator.vault.FrontMatter.FRONT_MATTER_MARKER;
import static nl.ulso.markdown_curator.vault.MarkdownTokenizer.TokenType.*;
import static nl.ulso.markdown_curator.vault.QueryBlock.QUERY_OUTPUT_PREFIX;

/**
 * Parses a list of {@link String}s into a {@link Document}. This is <strong>not</strong> a full
 * parser for Markdown, nor is it ever intended to be. Markdown documents are only broken down
 * in high-level parts (or: fragments). The contents of the fragments are still plain text
 * (Markdown).
 * <p/>
 * The parser is a bit complicated because the document is turned into a tree of sections of
 * varying levels. Because the document object model is immutable, a section can only a constructed
 * after all its content has been parsed, recursively.
 * <p/>
 * The parser also tries to protect against accidents. Front matter, code blocks and queries need
 * to be closed with a specific marker. If this marker is missing, the parser treats the block
 * as text. In case of doubt, that's the safest bet.
 */
final class DocumentParser
{
    private final String name;
    private final long lastModified;
    private final List<String> lines;
    private final Map<Integer, List<Fragment>> fragments;
    private final Deque<MarkdownTokenizer.HeaderLineToken> headers;

    public DocumentParser(String name, long lastModified, List<String> lines)
    {
        this.name = name;
        this.lastModified = lastModified;
        this.lines = lines;
        fragments = new HashMap<>();
        headers = new ArrayDeque<>();
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
            if (type == FRONT_MATTER)
            {
                frontMatterEnd = token.lineIndex() + 1;
                continue;
            }
            if (frontMatterEnd != -1)
            {
                fragments.get(0).add(createFragment(FRONT_MATTER, 0, frontMatterEnd));
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
            if (type == TEXT || type == CODE || type == QUERY)
            {
                fragmentStart = token.lineIndex();
                fragmentType = type;
                continue;
            }
            if (type == HEADER)
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
            if (type == END_OF_DOCUMENT)
            {
                while (!headers.isEmpty())
                {
                    processSection(token.lineIndex());
                }
                ensureFrontMatterIsPresent();
            }
        }
        var document = new Document(name, lastModified, fragments.get(0), lines);
        linkFragmentsToDocument(document);
        return document;
    }

    private void linkFragmentsToDocument(Document document)
    {
        var visitor = new BreadthFirstVaultVisitor()
        {
            @Override
            public void visit(FrontMatter frontMatter)
            {
                linkFragment(frontMatter);
            }

            @Override
            public void visit(Section section)
            {
                linkFragment(section);
                super.visit(section);
            }

            @Override
            public void visit(CodeBlock codeBlock)
            {
                linkFragment(codeBlock);
            }

            @Override
            public void visit(QueryBlock queryBlock)
            {
                linkFragment(queryBlock);
            }

            @Override
            public void visit(TextBlock textBlock)
            {
                linkFragment(textBlock);
            }

            private void linkFragment(DocumentHolder documentHolder)
            {
                documentHolder.setDocument(document);
            }
        };
        document.accept(visitor);
    }

    private Fragment createFragment(MarkdownTokenizer.TokenType type, int start, int end)
    {
        var subList = lines.subList(start, end);
        int size = subList.size();
        switch (type)
        {
            case FRONT_MATTER:
                if (size > 1 && subList.get(size - 1).contentEquals(FRONT_MATTER_MARKER))
                {
                    return new FrontMatter(subList);
                }
                else
                {
                    return new TextBlock(subList);
                }
            case CODE:
                if (size > 1 && subList.get(size - 1).contentEquals(CODE_MARKER))
                {
                    return new CodeBlock(lines.subList(start, end));
                }
                else
                {
                    return new TextBlock(lines.subList(start, end));
                }
            case QUERY:
                if (size > 1 && subList.get(size - 1).startsWith(QUERY_OUTPUT_PREFIX))
                {
                    return new QueryBlock(subList, start);
                }
                else
                {
                    return new TextBlock(lines.subList(start, end));
                }
            case TEXT:
                return new TextBlock(lines.subList(start, end));
            default:
                throw new IllegalStateException("Unsupported type " + type);
        }
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
