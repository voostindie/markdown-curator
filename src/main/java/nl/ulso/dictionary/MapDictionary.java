package nl.ulso.dictionary;

import nl.ulso.date.LocalDates;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/// Dictionary implementation on top of a map. Even though the map may contain many different types
/// of values, the dictionary only supports scalars ([String], [Integer],
/// [Boolean], [LocalDate]), lists of those, and nested dictionaries.
///
/// Nested dictionaries are stored as raw maps internally, so that serialization and equality work
/// as expected. They are exposed as [Dictionary] objects, through both [#getProperty(String)] and
/// [#dictionary(String)].
///
/// Trying to read a value that has a different type than then the type being requested results in
/// default values: either the value provided by the user (for scalars), or the empty lists (for
/// lists of scalars).
///
/// In other words: this dictionary never barfs on type mismatches. It always returns exactly what
/// you expect.
///
/// Lists and scalars are interchangeable in that:
///
///   - When reading a scalar from a list, you get the first item.
///   - When reading a list from a scalar, you get a one-element list with the scalar.
///
/// Internally, the dictionary is kept in a [TreeMap], to ensure that serializing a dictionary,
/// for example, with [#toYamlString()] is predictable: properties are always sorted on name.
class MapDictionary
        implements MutableDictionary
{
    private final Map<String, Object> map;

    MapDictionary()
    {
        this.map = new TreeMap<>();
    }

    MapDictionary(Map<String, Object> map)
    {
        this.map = new TreeMap<>(map);
    }

    Map<String, Object> map()
    {
        return this.map;
    }

    @Override
    public final boolean equals(Object o)
    {
        if (o instanceof MapDictionary dictionary)
        {
            return Objects.equals(map, dictionary.map);
        }
        return false;
    }

    @Override
    public final int hashCode()
    {
        return Objects.hash(map);
    }

    @Override
    public Set<String> propertyNames()
    {
        return unmodifiableSet(map.keySet());
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public Optional<Object> getProperty(String property)
    {
        var value = map.get(property);
        if (value instanceof Map<?, ?> nestedMap)
        {
            return Optional.of(nestedDictionary(nestedMap));
        }
        return Optional.ofNullable(value);
    }

    @Override
    public Dictionary dictionary(String property)
    {
        if (map.get(property) instanceof Map<?, ?> nestedMap)
        {
            return nestedDictionary(nestedMap);
        }
        return Dictionary.emptyDictionary();
    }

    /// Wraps a raw map as a dictionary. YAML allows non-string keys; these are converted to
    /// strings. Values are kept as is, including `null`s, so that the set of property names is
    /// preserved.
    private static Dictionary nestedDictionary(Map<?, ?> nestedMap)
    {
        if (nestedMap.isEmpty())
        {
            return Dictionary.emptyDictionary();
        }
        var copy = new HashMap<String, Object>();
        nestedMap.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return new MapDictionary(copy);
    }

    /// Converts a dictionary to a raw map, recursively, so that it can be stored as a property
    /// value.
    private static Map<String, Object> toMap(Dictionary dictionary)
    {
        if (dictionary instanceof MapDictionary mapDictionary)
        {
            return new TreeMap<>(mapDictionary.map);
        }
        var result = new TreeMap<String, Object>();
        dictionary.propertyNames().forEach(name ->
            dictionary.getProperty(name).ifPresent(value ->
                result.put(name, value instanceof Dictionary nested ? toMap(nested) : value)));
        return result;
    }

    @Override
    public String string(String property, String defaultValue)
    {
        return safeGet(property, String.class, defaultValue);
    }

    @Override
    public int integer(String property, int defaultValue)
    {
        return safeGet(property, Integer.class, defaultValue);
    }

    @Override
    public LocalDate date(String property, LocalDate defaultDate)
    {
        var dates = listOfDates(property);
        if (dates.isEmpty())
        {
            return defaultDate;
        }
        return dates.getFirst();
    }

    @Override
    public boolean bool(String property, boolean defaultValue)
    {
        return safeGet(property, Boolean.class, defaultValue);
    }

    @Override
    public List<String> listOfStrings(String property)
    {
        return safeGetList(property, String.class);
    }

    @Override
    public List<Integer> listOfIntegers(String property)
    {
        return safeGetList(property, Integer.class);
    }

    @Override
    public List<LocalDate> listOfDates(String property)
    {
        return safeGetList(property, String.class).stream()
                .map(LocalDates::parseDateOrNull)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean hasProperty(String property)
    {
        return map.containsKey(property);
    }

    @Override
    public String toYamlString()
    {
        var settings = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .build();
        return new Dump(settings).dumpToString(map);
    }

    protected <T> T safeGet(String property, Class<? extends T> propertyClass, T defaultValue)
    {
        Object value = map.get(property);
        if (value == null)
        {
            return defaultValue;
        }
        if (value instanceof List<?> list)
        {
            if (list.isEmpty())
            {
                return defaultValue;
            }
            value = list.getFirst();
        }
        if (!propertyClass.isInstance(value))
        {
            return defaultValue;
        }
        return propertyClass.cast(value);
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> safeGetList(String property, Class<? extends T> propertyClass)
    {
        Object value = map.get(property);
        if (value == null)
        {
            return emptyList();
        }
        if (!(value instanceof List<?>))
        {
            value = List.of(value);
        }
        List<?> list = (List<?>) value;
        if (list.isEmpty())
        {
            return emptyList();
        }
        var first = list.getFirst();
        if (!propertyClass.isInstance(first))
        {
            return emptyList();
        }
        return (List<T>) list;
    }

    @Override
    public void removeProperty(String property)
    {
        map.remove(property);
    }

    @Override
    public void setProperty(String property, Object value)
    {
        if (requireNonNull(value) instanceof Dictionary dictionary)
        {
            map.put(property, toMap(dictionary));
        }
        else
        {
            map.put(property, value);
        }
    }
}
