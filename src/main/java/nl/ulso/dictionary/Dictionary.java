package nl.ulso.dictionary;

import java.time.LocalDate;
import java.util.*;

import static java.util.Objects.requireNonNull;

/// Simple read-only map of key-value pairs.
///
/// Keys are always strings. Values are strings, integers, dates, lists of these, or nested
/// dictionaries. That's it!
///
/// [#getProperty(String)] returns nested dictionaries as [Dictionary] objects, never as raw maps.
///
/// There are no ways to in(tro)spect the dictionary. You need to know what you want to ask for,
/// and you are guaranteed to always get a valid answer.
public interface Dictionary
{
    static Dictionary emptyDictionary()
    {
        return EmptyDictionary.INSTANCE;
    }

    static MutableDictionary mutableDictionary()
    {
        return new MapDictionary();
    }

    /// Constructs a dictionary from YAML.
    ///
    /// If the parsing fails, the resulting dictionary is empty; it doesn't throw!
    ///
    /// The list of lines **may** contain YAML document markers. The first document
    /// will be extracted.
    ///
    /// @param lines Lines to parse as YAML.
    /// @return dictionary parsed from the YAML; can be empty!
    static Dictionary yamlDictionary(List<String> lines)
    {
        if (requireNonNull(lines).isEmpty())
        {
            return emptyDictionary();
        }
        return new YamlDictionary(lines);
    }

    /// @see #yamlDictionary(List)
    ///
    /// The list of lines **may not** contain YAML document markers. The string is
    /// expected to be a single YAML document.
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
        return new MapDictionary(new HashMap<>(map));
    }

    static MutableDictionary mutableDictionary(Dictionary source)
    {
        if (source instanceof MapDictionary sourceMap)
        {
            return new MapDictionary(sourceMap.map());
        }
        else
        {
            var dictionary = new MapDictionary();
            source.propertyNames().forEach(propertyName ->
                source.getProperty(propertyName).ifPresent(propertyValue ->
                    dictionary.setProperty(propertyName, propertyValue)));
            return dictionary;
        }
    }

    Set<String> propertyNames();

    boolean isEmpty();

    /// @return the raw value of the property, if any; nested dictionaries are returned as
    /// [Dictionary] objects.
    Optional<Object> getProperty(String name);

    String string(String property, String defaultValue);

    int integer(String property, int defaultValue);

    LocalDate date(String property, LocalDate defaultDate);

    boolean bool(String property, boolean defaultValue);

    List<String> listOfStrings(String property);

    List<Integer> listOfIntegers(String property);

    List<LocalDate> listOfDates(String property);

    /// @return the nested dictionary for the property; never `null`. If the property has no value
    /// or its value is not a dictionary, the result is an empty dictionary.
    Dictionary dictionary(String property);

    boolean hasProperty(String property);

    String toYamlString();
}
