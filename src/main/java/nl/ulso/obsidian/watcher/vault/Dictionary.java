package nl.ulso.obsidian.watcher.vault;

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
     *
     * @param lines Lines to parse as YAML.
     * @return dictionary parsed from the YAML; can be empty!
     */
    static Dictionary yamlDictionary(List<String> lines)
    {
        return new YamlDictionary(requireNonNull(lines));
    }

    boolean isEmpty();

    String string(String property, String defaultValue);

    int integer(String property, int defaultValue);

    Date date(String property, Date defaultDate);

    List<String> listOfStrings(String property);

    List<Integer> listOfIntegers(String property);

    List<Date> listOfDates(String property);
}
