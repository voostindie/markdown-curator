package nl.ulso.macu.vault;

import java.util.Date;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Simple read-only map of key-value pairs.
 * <p>
 * Keys are always strings. Values are strings, integers, dates or lists of these. That's it!
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

    boolean isEmpty();

    String string(String property, String defaultValue);

    int integer(String property, int defaultValue);

    Date date(String property, Date defaultDate);

    boolean bool(String property, boolean defaultValue);

    List<String> listOfStrings(String property);

    List<Integer> listOfIntegers(String property);

    List<Date> listOfDates(String property);
}
