package nl.ulso.curator.vault;

import nl.ulso.curator.vault.MarkdownTokenizer.LineToken;

import java.util.*;

import static nl.ulso.curator.vault.CodeBlock.CODE_MARKER;
import static nl.ulso.curator.vault.FrontMatter.FRONT_MATTER_MARKER;
import static nl.ulso.curator.vault.MarkdownTokenizer.LineToken.*;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenStatus.CONTENT;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenStatus.END;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenStatus.START;
import static nl.ulso.curator.vault.MarkdownTokenizer.TokenType.*;
import static nl.ulso.curator.vault.QueryBlock.QUERY_CONFIGURATION_PREFIX;
import static nl.ulso.curator.vault.QueryBlock.QUERY_OUTPUT_POSTFIX;
import static nl.ulso.curator.vault.QueryBlock.QUERY_OUTPUT_PREFIX;
import static nl.ulso.curator.vault.Section.HEADER_PATTERN;

/// Simple tokenizer for Markdown documents, Vincent flavored; the document is tokenized line by
/// line. It's not any fancier than that, on purpose.
///
/// What's Vincent Flavored Markdown (VFM), you ask? Well, it's basically GitHub Flavored Markdown,
/// with some changes. These are:
///
/// - Optional YAML front matter (between "---").
/// - Only ATX (#) headers, without the optional closing sequence of #'s.
/// - Headers are always aligned to the left margin.
/// - Code is always in code blocks surrounded with backticks.
/// - Queries can be defined in HTML comments, for this tool to process. See [QueryBlock]
///
class MarkdownTokenizer
    implements Iterable<LineToken>
{
    private final List<String> lines;

    enum TokenType
    {
        FRONT_MATTER,
        HEADER,
        TEXT,
        CODE,
        QUERY,
        END_OF_DOCUMENT
    }

    enum TokenStatus
    {
        START,
        CONTENT,
        END
    }

    static sealed class LineToken
    {
        private final int lineIndex;
        private final TokenType tokenType;
        private final TokenStatus tokenStatus;

        private LineToken(int lineIndex, TokenType tokenType, TokenStatus tokenStatus)
        {
            this.lineIndex = lineIndex;
            this.tokenType = tokenType;
            this.tokenStatus = tokenStatus;
        }

        int lineIndex()
        {
            return lineIndex;
        }

        TokenType tokenType()
        {
            return tokenType;
        }

        TokenStatus tokenStatus()
        {
            return tokenStatus;
        }

        static LineToken frontMatter(int lineIndex, TokenStatus tokenStatus)
        {
            return new LineToken(lineIndex, FRONT_MATTER, tokenStatus);
        }

        static LineToken header(int lineIndex, int level, String title)
        {
            return new HeaderLineToken(lineIndex, level, title);
        }

        static LineToken text(int lineIndex)
        {
            return new LineToken(lineIndex, TEXT, CONTENT);
        }

        static LineToken code(int lineIndex, TokenStatus tokenStatus)
        {
            return new LineToken(lineIndex, CODE, tokenStatus);
        }

        static LineToken query(int lineIndex, TokenStatus tokenStatus)
        {
            return new LineToken(lineIndex, QUERY, tokenStatus);
        }

        static LineToken documentEnd(int size)
        {
            return new LineToken(size, END_OF_DOCUMENT, END);
        }

    }

    static final class HeaderLineToken
        extends LineToken
    {
        private final int level;

        private final String title;

        private HeaderLineToken(int lineIndex, int level, String title)
        {
            super(lineIndex, HEADER, CONTENT);
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
        TEXT,
        FRONT_MATTER,
        CODE,
        QUERY
    }

    @Override
    public Iterator<LineToken> iterator()
    {
        return new Iterator<>()
        {
            private final int size = lines.size();
            private Mode mode = Mode.TEXT;
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
                if (i > size)
                {
                    throw new NoSuchElementException();
                }
                index++;
                if (i == size)
                {
                    return documentEnd(size);
                }
                var line = lines.get(i);
                if (i == 0 && line.contentEquals(FRONT_MATTER_MARKER))
                {
                    mode = Mode.FRONT_MATTER;
                    return frontMatter(i, START);
                }
                if (mode == Mode.FRONT_MATTER)
                {
                    var status = CONTENT;
                    if (line.contentEquals(FRONT_MATTER_MARKER))
                    {
                        status = END;
                        mode = Mode.TEXT;
                    }
                    return frontMatter(i, status);
                }
                if (mode == Mode.TEXT && line.startsWith(QUERY_CONFIGURATION_PREFIX))
                {
                    mode = Mode.QUERY;
                    return query(i, START);
                }
                if (mode == Mode.QUERY)
                {
                    var status = CONTENT;
                    if (line.startsWith(QUERY_CONFIGURATION_PREFIX))
                    {
                        status = START;
                    }
                    else if (line.startsWith(QUERY_OUTPUT_PREFIX) &&
                             line.endsWith(QUERY_OUTPUT_POSTFIX))
                    {
                        status = END;
                        mode = Mode.TEXT;
                    }
                    return query(i, status);
                }
                if (mode == Mode.TEXT && line.startsWith(CODE_MARKER))
                {
                    mode = Mode.CODE;
                    return code(i, START);
                }
                if (mode == Mode.CODE)
                {
                    var status = CONTENT;
                    if (line.contentEquals(CODE_MARKER))
                    {
                        status = END;
                        mode = Mode.TEXT;
                    }
                    return code(i, status);
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
