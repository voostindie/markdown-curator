package nl.ulso.curator.vault;

import nl.ulso.curator.vault.MarkdownTokenizer.HeaderLineToken;
import nl.ulso.curator.vault.MarkdownTokenizer.TokenType;

import java.util.*;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenStatus.END;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenStatus.START;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenType.END_OF_DOCUMENT;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenType.HEADER;

/// Parses a list of [String]s into a [Document]. This is **not** a full parser for Markdown, nor is
/// it ever intended to be. Markdown documents are only broken down in high-level parts (or:
/// fragments). The contents of the fragments are still plain text (Markdown).
///
/// The parser is a bit complicated because the document is turned into a tree of sections of
/// varying levels. Because the document object model is immutable, a section can only a constructed
/// after all its content has been parsed, recursively.
///
/// The parser also tries to protect against accidents. Front matter, code blocks and queries need
/// to be closed with a specific marker. If this marker is missing, the parser treats the block as
/// text. In case of doubt, that's the safest bet.
final class DocumentParser
{
    private final String name;
    private final long lastModified;
    private final List<String> lines;
    private final Map<Integer, List<Fragment>> fragments;
    private final Deque<HeaderLineToken> headers;

    public DocumentParser(String name, long lastModified, List<String> lines)
    {
        this.name = name;
        this.lastModified = lastModified;
        this.lines = lines;
        this.fragments = new HashMap<>();
        this.headers = new ArrayDeque<>();
    }

    Document parse()
    {
        fragments.clear();
        fragments.put(0, new ArrayList<>());
        headers.clear();
        var level = 0;
        var startIndex = 0;
        for (var token : new MarkdownTokenizer(lines))
        {
            var type = token.tokenType();
            var status = token.tokenStatus();
            var lineIndex = token.lineIndex();
            if (type == HEADER)
            {
                processText(level, startIndex, lineIndex);
                var header = (HeaderLineToken) token;
                while (header.level() <= level)
                {
                    level = processSection();
                }
                level = header.level();
                fragments.put(level, new ArrayList<>());
                headers.push(header);
                startIndex = lineIndex + 1;
            }
            else if (type == END_OF_DOCUMENT)
            {
                processText(level, startIndex, lineIndex);
                while (!headers.isEmpty())
                {
                    processSection();
                }
                ensureFrontMatterIsPresent();
            }
            else if (status == START)
            {
                processText(level, startIndex, lineIndex);
                startIndex = lineIndex;
            }
            else if (status == END)
            {
                processFragment(level, type, startIndex, lineIndex + 1);
                startIndex = lineIndex + 1;
            }
        }
        var document = new Document(name, lastModified, fragments.get(0));
        updateInternalReferences(document);
        return document;
    }

    private int processSection()
    {
        var header = headers.pop();
        var previousLevel = headers.isEmpty() ? 0 : headers.peek().level();
        fragments.get(previousLevel).add(
            new Section(header.level(), header.title(), fragments.get(header.level())));
        return previousLevel;
    }

    private void processText(int level, int startIndex, int endIndex)
    {
        if (startIndex < endIndex)
        {
            fragments.get(level).add(new TextBlock(lines.subList(startIndex, endIndex)));
        }
    }

    private void processFragment(int level, TokenType type, int startIndex, int endIndex)
    {
        var subList = lines.subList(startIndex, endIndex);
        var fragment = switch (type)
        {
            case FRONT_MATTER -> new FrontMatter(subList);
            case CODE -> new CodeBlock(subList);
            case QUERY -> new QueryBlock(subList);
            default -> throw new IllegalStateException("Unsupported type " + type);
        };
        fragments.get(level).add(fragment);
    }

    private void ensureFrontMatterIsPresent()
    {
        var toplevel = fragments.get(0);
        if (toplevel.isEmpty() || !(toplevel.getFirst() instanceof FrontMatter))
        {
            toplevel.addFirst(new FrontMatter(emptyList()));
        }
    }

    private void updateInternalReferences(Document document)
    {
        var visitor = new BreadthFirstVaultVisitor()
        {
            private Section currentSection;

            @Override
            public void visit(FrontMatter frontMatter)
            {
                linkFragment(frontMatter);
            }

            @Override
            public void visit(Section section)
            {
                linkFragment(section);
                var tempSection = currentSection;
                currentSection = section;
                super.visit(section);
                currentSection = tempSection;
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

            private void linkFragment(FragmentBase fragment)
            {
                fragment.setInternalReferences(document, currentSection);
            }
        };
        document.accept(visitor);
    }
}
