package nl.ulso.obsidian.watcher.vault;

import java.util.List;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Container for a list of {@link String} lines. On construction a sublist is created by dropping
 * all blank lines at the beginning and the end, effectively trimming the input.
 */
abstract class LineContainer
{
    private final List<String> lines;

    LineContainer(List<String> lines)
    {
        this.lines = unmodifiableList(shrink(requireNonNull(lines)));
    }

    private static List<String> shrink(List<String> lines)
    {
        var from = 0;
        var to = lines.size();
        while (from < to && lines.get(from).isBlank())
        {
            from++;
        }
        while (to > from && lines.get(to - 1).isBlank())
        {
            to--;
        }
        return lines.subList(from, to);
    }

    public List<String> lines()
    {
        return lines;
    }

    boolean isEmpty()
    {
        return lines.isEmpty();
    }

    public String content()
    {
        return join(lineSeparator(), lines);
    }
}
