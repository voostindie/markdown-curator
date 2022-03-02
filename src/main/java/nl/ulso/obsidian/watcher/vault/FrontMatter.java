package nl.ulso.obsidian.watcher.vault;

import java.util.*;

import static nl.ulso.obsidian.watcher.vault.Dictionary.emptyDictionary;
import static nl.ulso.obsidian.watcher.vault.Dictionary.yamlDictionary;

/**
 * Represents the front matter section in a {@link Document}. A {@link Document} object
 * <strong>always</strong> has such a section (as the first object in the list of fragments),
 * even if the underlying document has none; in that case the front matter is empty.
 * <p/>
 * This class wraps a {@link Dictionary}. For ease of use it implements its interface as well.
 */
public final class FrontMatter
        extends LineContainer
        implements Fragment, Dictionary
{
    private final Dictionary dictionary;

    public FrontMatter(List<String> lines)
    {
        super(lines);
        if (lines.isEmpty())
        {
            dictionary = emptyDictionary();
        }
        else
        {
            dictionary = yamlDictionary(lines);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof FrontMatter frontMatter)
        {
            return Objects.equals(dictionary, frontMatter.dictionary)
                    && Objects.equals(lines(), frontMatter.lines());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(dictionary, lines());
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    public Dictionary dictionary()
    {
        return dictionary;
    }

    @Override
    public boolean isEmpty()
    {
        return dictionary.isEmpty();
    }

    @Override
    public String string(String property, String defaultValue)
    {
        return dictionary.string(property, defaultValue);
    }

    @Override
    public int integer(String property, int defaultValue)
    {
        return dictionary.integer(property, defaultValue);
    }

    @Override
    public Date date(String property, Date defaultDate)
    {
        return dictionary.date(property, defaultDate);
    }

    @Override
    public List<String> listOfStrings(String property)
    {
        return dictionary.listOfStrings(property);
    }

    @Override
    public List<Integer> listOfIntegers(String property)
    {
        return dictionary.listOfIntegers(property);
    }

    @Override
    public List<Date> listOfDates(String property)
    {
        return dictionary.listOfDates(property);
    }
}
