package nl.ulso.obsidian.watcher.vault;

import java.util.List;
import java.util.Set;

public final class Section
        extends FragmentContainer
        implements Fragment
{
    private final static Set<Character> INVALID_ANCHOR_CHARACTERS = Set.of(
            '<', '>', ',', '.', ';', '[', ']', '!', '@', '#', '$', '%', '^', '&', '*',
            '(', ')', '=', '+', '/', '?', '{', '}', '\\', '~', '`');

    private final int level;
    private final String title;
    private final String anchor;

    Section(int level, String title, List<String> lines, List<Fragment> fragments)
    {
        super(lines, fragments);
        this.level = level;
        this.title = title;
        this.anchor = createAnchor(title);
    }

    private String createAnchor(String title)
    {
        return title.chars().filter(Section::isValidAnchorCharacter)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    // TODO: fix this method for Obsidian; this needs reverse engineering.
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

    public String anchor()
    {
        return anchor;
    }

    @Override
    public void accept(Visitor visitor)
    {
        visitor.visitSection(this);
    }
}
