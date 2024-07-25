package nl.ulso.markdown_curator.vault;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static java.lang.System.lineSeparator;
import static nl.ulso.markdown_curator.vault.Dictionary.emptyDictionary;
import static nl.ulso.markdown_curator.vault.Dictionary.yamlDictionary;

/**
 * Represents the front matter section in a {@link Document}. A {@link Document} object
 * <strong>always</strong> has such a section (as the first object in the list of fragments),
 * even if the underlying document has none; in that case the front matter is empty.
 * <p/>
 * This class wraps a {@link nl.ulso.markdown_curator.vault.Dictionary}. For ease of use it
 * implements its interface as well.
 * <p/>
 * This implementation holds both the original content (a {@link String}) as well as the processed
 * content (a {@link Dictionary}) of the front matter in memory. The original content is needed
 * to ensure that front matter is written back to disk unchanged, however the document author
 * formatted it.
 */
public final class FrontMatter
        extends FragmentBase
        implements Fragment, Dictionary
{
    static final String FRONT_MATTER_MARKER = "---";

    private final String markdown;
    private final Dictionary dictionary;

    FrontMatter(List<String> lines)
    {
        if (lines.isEmpty())
        {
            dictionary = emptyDictionary();
            markdown = "";
        }
        else
        {
            dictionary = yamlDictionary(lines);
            markdown = String.join(lineSeparator(), lines) + lineSeparator();
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof FrontMatter frontMatter)
        {
            return Objects.equals(markdown, frontMatter.markdown) &&
                   Objects.equals(dictionary, frontMatter.dictionary);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(markdown, dictionary);
    }

    public String markdown()
    {
        return markdown;
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
    public LocalDate date(String property, LocalDate defaultDate)
    {
        return dictionary.date(property, defaultDate);
    }

    @Override
    public boolean bool(String property, boolean defaultValue)
    {
        return dictionary.bool(property, defaultValue);
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
    public List<LocalDate> listOfDates(String property)
    {
        return dictionary.listOfDates(property);
    }

    @Override
    public boolean hasProperty(String property)
    {
        return dictionary.hasProperty(property);
    }
}
