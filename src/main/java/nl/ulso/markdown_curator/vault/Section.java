package nl.ulso.markdown_curator.vault;

import java.util.*;
import java.util.regex.Pattern;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static nl.ulso.emoji.EmojiFilter.stripEmojis;

public final class Section
        extends FragmentContainer
        implements Fragment
{
    static final Pattern HEADER_PATTERN = compile("^(#{1,6}) (.*)$");

    // These characters are filtered out by Obsidian in anchors; reverse engineered!
    // As of Obsidian 1.0 this list is a lot shorter; just a few characters are problematic.
    private static final Set<Character> INVALID_ANCHOR_CHARACTERS = Set.of('#', '*', '[', ']', '`');

    private final int level;
    private final String title;
    private final String sortableTitle;
    private final String anchor;

    Section(int level, String title, List<Fragment> fragments)
    {
        super(fragments);
        if (level < 1)
        {
            throw new IllegalArgumentException("Minimum allowed section level is 1");
        }
        this.level = level;
        this.title = requireNonNull(title);
        this.sortableTitle = stripEmojis(title).trim();
        this.anchor = createAnchor(title);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof Section section)
        {
            return Objects.equals(level, section.level)
                    && Objects.equals(title, section.title)
                    && Objects.equals(fragments(), section.fragments());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(level, title, fragments());
    }

    public static String createAnchor(String title)
    {
        return title.chars().filter(Section::isValidAnchorCharacter)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .trim()
                .replaceAll("\\s+"," ");
    }

    private static boolean isValidAnchorCharacter(int c)
    {
        return !INVALID_ANCHOR_CHARACTERS.contains((char) c);
    }

    public int level()
    {
        return level;
    }

    public String title()
    {
        return title;
    }

    public String sortableTitle()
    {
        return sortableTitle;
    }

    public String anchor()
    {
        return anchor;
    }

    public String markdown()
    {
        return "#".repeat(level) + " " + title + lineSeparator();
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }
}
