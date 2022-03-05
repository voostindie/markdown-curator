package nl.ulso.obsidian.watcher.vault;

import nl.ulso.obsidian.watcher.vault.MarkdownTokenizer.LineToken;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static nl.ulso.obsidian.watcher.vault.MarkdownTokenizer.LineToken.*;
import static nl.ulso.obsidian.watcher.vault.MarkdownTokenizer.TokenType.*;

/**
 * Simple tokenizer for Markdown document, Vincent flavored; the document is tokenized line by line.
 * It's not any fancier than that, on purpose.
 * <p/>
 * What's Vincent Flavored Markdown (VFM), you ask? Well, it's basically GitHub Flavored Markdown,
 * with some changes. These are:
 * <ul>
 *   <li>Optional YAML front matter (between "---")</li>
 *   <li>Only ATX (#) headers, without the optional closing sequence of #'s</li>
 *   <li>Headers are always aligned to the left margin</li>
 *   <li>Code is always in code blocks surrounded with backticks</li>
 *   <li>Queries can be defined in HTML comments, for this tool to process. See {@link Query}</li>
 * </ul>
 */
class MarkdownTokenizer
        implements Iterable<LineToken>
{
    private static final String FRONT_MATTER_MARKER = "---";
    private static final Pattern HEADER_PATTERN = compile("^(#{1,6}) (.*)$");
    private static final String CODE_MARKER = "```";
    static final String QUERY_BEGIN_MARKER = "<!--query";
    static final String QUERY_CLOSING = "-->";
    private static final String QUERY_END_MARKER = "<!--/query";

    private final List<String> lines;

    enum TokenType
    {
        FRONT_MATTER,
        HEADER,
        TEXT,
        CODE,
        QUERY_DEFINITION,
        QUERY_RESULT,
        QUERY_END,
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

        static LineToken code(int lineIndex)
        {
            return new LineToken(lineIndex, CODE);
        }

        static LineToken queryDefinition(int lineIndex)
        {
            return new LineToken(lineIndex, QUERY_DEFINITION);
        }

        static LineToken queryResult(int lineIndex)
        {
            return new LineToken(lineIndex, QUERY_RESULT);
        }

        static LineToken queryEnd(int lineIndex)
        {
            return new LineToken(lineIndex, QUERY_END);
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

    public MarkdownTokenizer(List<String> lines)
    {
        this.lines = Objects.requireNonNull(lines);
    }

    private enum Mode
    {
        NORMAL,
        FRONT_MATTER,
        CODE,
        QUERY_DEFINITION,
        QUERY_OUTPUT
    }

    @Override
    public Iterator<LineToken> iterator()
    {
        return new Iterator<>()
        {
            private final int size = lines.size();
            private Mode mode = Mode.NORMAL;
            private int index = 0;

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
                if (i == size)
                {
                    return documentEnd(size);
                }
                var line = lines.get(i);
                if (i == 0 && line.contentEquals(FRONT_MATTER_MARKER))
                {
                    mode = Mode.FRONT_MATTER;
                    return frontMatter(i);
                }
                if (mode == Mode.FRONT_MATTER)
                {
                    if (line.contentEquals(FRONT_MATTER_MARKER))
                    {
                        mode = Mode.NORMAL;
                    }
                    return frontMatter(i);
                }
                if (mode == Mode.NORMAL && line.startsWith(QUERY_BEGIN_MARKER))
                {
                    mode = line.endsWith(QUERY_CLOSING) ? Mode.QUERY_OUTPUT : Mode.QUERY_DEFINITION;
                    return queryDefinition(i);
                }
                if (mode == Mode.QUERY_DEFINITION)
                {
                    if (line.endsWith(QUERY_CLOSING))
                    {
                        mode = Mode.QUERY_OUTPUT;
                    }
                    return queryDefinition(i);
                }
                if (mode == Mode.QUERY_OUTPUT)
                {
                    if (line.startsWith(QUERY_END_MARKER) && line.endsWith(QUERY_CLOSING))
                    {
                        mode = Mode.NORMAL;
                        return queryEnd(i);
                    }
                    return queryResult(i);
                }
                if (mode == Mode.NORMAL && line.startsWith(CODE_MARKER))
                {
                    mode = Mode.CODE;
                    return code(i);
                }
                if (mode == Mode.CODE)
                {
                    if (line.contentEquals(CODE_MARKER))
                    {
                        mode = Mode.NORMAL;
                    }
                    return code(i);
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
