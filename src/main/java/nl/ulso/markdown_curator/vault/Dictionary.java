package nl.ulso.markdown_curator.vault;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Simple read-only map of key-value pairs.
 * <p/>
 * Keys are always strings. Values are strings, integers, dates or lists of these. That's it!
 * <p/>
 * There are no ways to in(tro)spect the dictionary. You need to know what you want to ask for,
 * and you are guaranteed to always get a valid answer.
 */
public interface Dictionary
{
    static Dictionary emptyDictionary()
    {
        return EmptyDictionary.INSTANCE;
    }

    /**
     * Constructs a dictionary from YAML.
     * <p>
     * If the parsing fails, the resulting dictionary is empty; it doesn't throw!
     * <p/>
     * The list of lines <strong>may</strong> contain YAML document markers. The first document
     * will be extracted.
     *
     * @param lines Lines to parse as YAML.
     * @return dictionary parsed from the YAML; can be empty!
     */
    static Dictionary yamlDictionary(List<String> lines)
    {
        if (requireNonNull(lines).isEmpty())
        {
            return emptyDictionary();
        }
        return new YamlDictionary(lines);
    }

    /**
     * @see #yamlDictionary(List)
     * <p/>
     * The list of lines <strong>may not</strong> contain YAML document markers. The string is
     * expected to be a single YAML document.
     */
    static Dictionary yamlDictionary(String string)
    {
        if (requireNonNull(string).isBlank())
        {
            return emptyDictionary();
        }
        return new YamlDictionary(string);
    }

    static Dictionary mapDictionary(Map<String, ?> map)
    {
        if (requireNonNull(map).isEmpty())
        {
            return emptyDictionary();
        }
        return new MapDictionary(requireNonNull(map));
    }

    boolean isEmpty();

    String string(String property, String defaultValue);

    int integer(String property, int defaultValue);

    LocalDate date(String property, LocalDate defaultDate);

    boolean bool(String property, boolean defaultValue);

    List<String> listOfStrings(String property);

    List<Integer> listOfIntegers(String property);

    List<LocalDate> listOfDates(String property);

    boolean hasProperty(String property);
}
