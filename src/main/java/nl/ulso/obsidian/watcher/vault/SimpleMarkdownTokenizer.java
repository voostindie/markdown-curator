package nl.ulso.obsidian.watcher.vault;

import nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.LineToken;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.LineToken.documentEnd;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.LineToken.frontMatter;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.LineToken.header;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.LineToken.text;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.END_OF_DOCUMENT;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.FRONT_MATTER;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.HEADER;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.TEXT;

/**
 * Simple tokenizer for Markdown document, Vincent flavored; the document is tokenized line by line.
 * It's not any fancier than that, on purpose.
 * <p/>
 * What's Vincent Flavored Markdown (VFM), you ask? Well, it's basically GitHub Flavored Markdown,
 * with some changes. These are:
 * <ul>
 *   <li>Extra: Optional YAML front matter (between "---")</li>
 *   <li>Only ATX (#) headers, without the optional closing sequence of #'s</li>
 *   <li>Headers are always aligned to the left margin</li>
 * </ul>
 */
class SimpleMarkdownTokenizer
        implements Iterable<LineToken>
{
    private static final String FRONT_MATTER_MARKER = "---";
    private static final String CODE_MARKER = "```";
    private static final Pattern HEADER_PATTERN = compile("^(#{1,6}) (.*)$");

    private final List<String> lines;

    enum TokenType
    {
        FRONT_MATTER,
        HEADER,
        TEXT,
        END_OF_DOCUMENT
    }

    static sealed class LineToken
    {
        private final int lineIndex;
        private final TokenType tokenType;

        private LineToken(int lineIndex, TokenType tokenType)
        {
            this.lineIndex = lineIndex;
            this.tokenType = tokenType;
        }

        int lineIndex()
        {
            return lineIndex;
        }

        TokenType tokenType()
        {
            return tokenType;
        }

        static LineToken frontMatter(int lineIndex)
        {
            return new LineToken(lineIndex, FRONT_MATTER);
        }

        static LineToken header(int lineIndex, int level, String title)
        {
            return new HeaderLineToken(lineIndex, level, title);
        }

        static LineToken text(int lineIndex)
        {
            return new LineToken(lineIndex, TEXT);
        }

        static LineToken documentEnd(int size)
        {
            return new LineToken(size, END_OF_DOCUMENT);
        }
    }

    static final class HeaderLineToken
            extends LineToken
    {
        private final int level;
        private final String title;

        private HeaderLineToken(int lineIndex, int level, String title)
        {
            super(lineIndex, HEADER);
            this.level = level;
            this.title = title;
        }

        int level()
        {
            return level;
        }

        String title()
        {
            return title;
        }
    }

    public SimpleMarkdownTokenizer(List<String> lines)
    {
        this.lines = Objects.requireNonNull(lines);
    }

    @Override
    public Iterator<LineToken> iterator()
    {
        return new Iterator<>()
        {
            private final int size = lines.size();
            private int index = 0;
            private boolean inFrontMatter = false;
            private boolean inCode = false;

            @Override
            public boolean hasNext()
            {
                return index <= size;
            }

            @Override
            public LineToken next()
            {
                var i = this.index;
                index++;
                if (i >= size)
                {
                    return documentEnd(size);
                }
                var line = lines.get(i);
                if (i == 0 && line.contentEquals(FRONT_MATTER_MARKER))
                {
                    inFrontMatter = true;
                    return frontMatter(i);
                }
                if (i > 0 && inFrontMatter && line.contentEquals(FRONT_MATTER_MARKER))
                {
                    inFrontMatter = false;
                    return frontMatter(i);
                }
                if (i > 0 && inFrontMatter)
                {
                    return frontMatter(i);
                }
                if (!inCode && line.startsWith(CODE_MARKER))
                {
                    inCode = true;
                    return text(i);
                }
                if (inCode && line.contentEquals(CODE_MARKER))
                {
                    inCode = false;
                    return text(i);
                }
                if (inCode)
                {
                    return text(i);
                }
                var matcher = HEADER_PATTERN.matcher(line);
                if (matcher.matches())
                {
                    var level = matcher.group(1).length();
                    var title = matcher.group(2);
                    return header(i, level, title);
                }
                return text(i);
            }
        };
    }
}
